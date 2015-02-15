package SystemA;

import java.io.IOException;
import java.io.PipedInputStream;
import java.util.HashSet;

/**
 * This method takes input from both the altitude converter and the
 * temperature converter, merging the inputstreams for the sink filter to
 * write to output.
 * This class is special in that it has TWO inputports, one in the superclass
 * and one as a private member of the subclass.
 * @author ligu
 *
 */
public class MergeFilterA extends FilterFramework {
	//The second inputport, this one takes data from the
	//Temperature converter
	private PipedInputStream InputReadPort2 = new PipedInputStream();
	
	/**
	 * Overload the superclass constructor
	 * @param codesSet
	 */
	public MergeFilterA(HashSet<Integer> codesSet) {
		setInputType(codesSet);
	}

	/**
	 * Runs the filter
	 */
	public void run() {
		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream

		/**
		 * Two measurements are taken concurrently, measurement2 is from inputport2,
		 * which would be the temperature data.
		 * measurement is from inputport1, which would be the altitude data.
		 */
		long measurement;				
		long measurement2;
		/**
		 * Two ID's are stored in similar fashion.
		 */
		int id;						
		int id2;
		
		int i;							// This is a loop counter
		
		/**
		 * Just as we need two byte arrays before, we need four this time.
		 */
		byte[] id_bytes;
		byte[] data_bytes;
		byte[] id_bytes2;
		byte[] data_bytes2;

		int byteswritten = 0;				// Number of bytes written to the stream.

		// Next we write a message to the terminal to let the world know we are alive...
		System.out.print( "\n" + this.getName() + "::Middle Reading ");

		while (true) {
			try {	
				/**
				 * Read four bytes of ID from the second input port
				 */
				id2 = 0;
				id_bytes2 = new byte[4];
				for (i=0; i<IdLength; i++ ){
					databyte = ReadFilterInputPort2();	// This is where we read the byte from the stream...

					id2 = id2 | (databyte & 0xFF);		// We append the byte on to ID...

					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id2 = id2 << 8;					// to make room for the next byte we append to the ID

					} // if
					id_bytes2[i] = databyte;
					bytesread++;						// Increment the byte count

				} // for
				
				/**
				 * Read four bytes of ID from the first input port
				 */
				id = 0;
				id_bytes = new byte[4];
				for (i=0; i<IdLength; i++ ){
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...

					id = id | (databyte & 0xFF);		// We append the byte on to ID...

					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID

					} // if
					id_bytes[i] = databyte;
					bytesread++;						// Increment the byte count

				} // for
			
				/**
				 * Read 8 bytes of measurements from the second input port.
				 * Buffering as necessary.
				 */
				measurement2 = 0;
				data_bytes2 = new byte[8];
				for (i=0; i<MeasurementLength; i++ ) {
					databyte = ReadFilterInputPort2();
					measurement2 = measurement2 | (databyte & 0xFF);	// We append the byte on to measurement...

					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement2 = measurement2 << 8;				// to make room for the next byte we append to the
																	// measurement
					} // if
					data_bytes2[i] = databyte;
					bytesread++;									// Increment the byte count

				} // if
				/**
				 * Read 8 bytes of measurements from the first input port.
				 * Buffering as necessary.
				 */
				measurement = 0;
				data_bytes = new byte[8];
				for (i=0; i<MeasurementLength; i++ ) {
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...

					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
																	// measurement
					} // if
					data_bytes[i] = databyte;
					bytesread++;									// Increment the byte count

				} // if
				
				/**
				 * The only data in id_bytes2 and data_bytes2 are
				 * time and temperature. We want to write them to the output
				 * either way.
				 */
				for (i = 0; i < 4; i++) {
					WriteFilterOutputPort(id_bytes2[i]);
					byteswritten++;
				}
				for (i = 0; i < 8; i++) {
					WriteFilterOutputPort(data_bytes2[i]);
					byteswritten++;
				}
				/** 
				 * We ONLY write ID and data from the first inputport if the
				 * measurements are altitude. We do not care about the time
				 * since the previous write would have covered it.
				 */
				if (id == 2) {
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(id_bytes[i]);
						byteswritten++;
					}
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						byteswritten++;
					}
				}
			} // try

			catch (EndOfStreamException e) {
				ClosePorts();
				ClosePorts2();
				System.out.print( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				break;

			} // catch

		} // while

	} // run
	
	/**
	 * This method DOES NOT override the superclass method. It simply allows us
	 * to read from the second input port.
	 * @return
	 * @throws EndOfStreamException
	 */
	byte ReadFilterInputPort2() throws EndOfStreamException {
		byte datum = 0;

		/***********************************************************************
		* Since delays are possible on upstream filters, we first wait until
		* there is data available on the input port. We check,... if no data is
		* available on the input port we wait for a quarter of a second and check
		* again. Note there is no timeout enforced here at all and if upstream
		* filters are deadlocked, then this can result in infinite waits in this
		* loop. It is necessary to check to see if we are at the end of stream
		* in the wait loop because it is possible that the upstream filter completes
		* while we are waiting. If this happens and we do not check for the end of
		* stream, then we could wait forever on an upstream pipe that is long gone.
		* Unfortunately Java pipes do not throw exceptions when the input pipe is
		* broken. So what we do here is to see if the upstream filter is alive.
		* if it is, we assume the pipe is still open and sending data. If the
		* filter is not alive, then we assume the end of stream has been reached.
		***********************************************************************/

		try
		{
			while (InputReadPort2.available()==0 )
			{
				if (EndOfInputStream())
				{
					throw new EndOfStreamException("End of input stream reached");

				} //if

				sleep(250);

			} // while

		} // try

		catch( EndOfStreamException Error )
		{
			throw Error;

		} // catch

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " Error in read port wait loop::" + Error );

		} // catch

		/***********************************************************************
		* If at least one byte of data is available on the input
		* pipe we can read it. We read and write one byte to and from ports.
		***********************************************************************/

		try
		{
			datum = (byte)InputReadPort2.read();
			return datum;

		} // try

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " Pipe read error::" + Error );
			return datum;

		} // catch

	} // ReadInputFilterPort2
	
	/**
	 * This method DOES NOT override the superclass method for closing ports.
	 * Where the super class method closes input port and the first output port,
	 * this method closes the second output port.
	 */
	void ClosePorts2() {
		try {
			InputReadPort2.close();
		}
		catch( Exception Error ) {
			System.out.println( "\n" + this.getName() + " ClosePorts error::" + Error );

		} // catch

	} // ClosePorts2
	
	/**
	 * This method OVERRIDES the superclass connect method. It does not take
	 * a generic FilterFramework, rather, it takes a specific Altitude Filter.
	 * @param Filter
	 * @param codes
	 * @throws IOException
	 */
	void Connect( AltConverterA Filter, HashSet<Integer> codes) throws IOException {
		Filter.validateConvertedCodes(codes);
		try
		{
			// Connect this filter's input to the upstream pipe's output stream

			getInputReadPort().connect( Filter.getOutputWritePort() );
			setInputFilter(Filter);

		} // try

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " FilterFramework error connecting::"+ Error );

		} // catch

	} // Connect
	
	/**
	 * This method OVERRIDES the superclass connect method. It does not take
	 * a generic FilterFramework, rather, it takes a specific Temperature Filter.
	 * @param Filter
	 * @param codes
	 * @throws IOException
	 */
	void Connect( TempConverterA Filter, HashSet<Integer> codes ) throws IOException {
		Filter.validateConvertedCodes(codes);
		try
		{
			// Connect this filter's input to the upstream pipe's output stream

			InputReadPort2.connect( Filter.getOutputWritePort() );
			setInputFilter(Filter);

		} // try

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " FilterFramework error connecting::"+ Error );

		} // catch

	} // Connect

	public PipedInputStream getInputReadPort2() {
		return InputReadPort2;
	}
} // MergerFilterA
