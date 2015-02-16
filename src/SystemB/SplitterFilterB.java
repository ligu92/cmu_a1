package SystemB;

import java.io.PipedOutputStream;
import java.util.HashSet;

/**
 * This is SplitterFilter for SystemA. This one is responsible for taking
 * the raw input stream from the source and split it up. In the case of
 * SystemA, time and altitude go to the AltConverter, time and temperature
 * goto the TempConverter filter. The other data don't go anywhere since we 
 * don't care about them in SystemA.
 * 
 * This class is somewhat special. We tried to preserve the original
 * Framework, which is suited to one inputport and one outputport.
 * Since our architecture calls for two outputports, we have to add
 * another private member to this filter. This calls for the overriding
 * of some methods of the superclass and some new methods.
 * @author ligu
 *
 */
public class SplitterFilterB extends FilterFramework {
	// These are the second and third outputports for the splitter.
	// The first outputport goes to the altitude converter, so this
	// This one goes to the temperature one.
	private PipedOutputStream OutputWritePort2 = new PipedOutputStream();
	// This one goes to Pressure filter.
	private PipedOutputStream OutputWritePort3 = new PipedOutputStream();
	
	/**
	 * Overload the superclass constructor
	 * @param codesSet
	 */
	public SplitterFilterB(HashSet<Integer> codesSet) {
		setInputType(codesSet);
	}

	/**
	 * Starts off the filter.
	 */
	public void run() {
		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream

		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter
		
		//We use two byte arrays to buffer the 4 bytes of ID and the 8 bytes of measurements
		byte[] id_bytes;
		byte[] data_bytes;

		int byteswritten = 0;				// Number of bytes written to the stream.

		// Next we write a message to the terminal to let the world know we are alive...

		System.out.print( "\n" + this.getName() + "::Middle Reading ");

		while (true) {
			try {		
				/**
				 * Similar to other filters, we read the 4 bytes of ID.
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
					// We add the 4 bytes of id into the byte array for buffering
					id_bytes[i] = databyte;
					bytesread++;						// Increment the byte count

				} // for

				/****************************************************************************
				// Here we read measurements. All measurement data is read as a stream of bytes
				// and stored as a long value. This permits us to do bitwise manipulation that
				// is neccesary to convert the byte stream into data words. Note that bitwise
				// manipulation is not permitted on any kind of floating point types in Java.
				// If the id = 0 then this is a time value and is therefore a long value - no
				// problem. However, if the id is something other than 0, then the bits in the
				// long value is really of type double and we need to convert the value using
				// Double.longBitsToDouble(long val) to do the conversion which is illustrated.
				// below.
				*****************************************************************************/
				measurement = 0;
				data_bytes = new byte[8];
				for (i=0; i<MeasurementLength; i++ )
				{
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...

					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
																	// measurement
					} // if
					// Again, note the buffering array
					data_bytes[i] = databyte;
					bytesread++;									// Increment the byte count

				} // if

				/**
				 * If the id is 0, that means we have time. According to our
				 * architecture, time data goes to both of the outputports.
				 * So we go through both the id and measurement byte arrays and
				 * write to both outputports.
				 */
				if ( id == 0 ) {
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(id_bytes[i]);
						WriteFilterOutputPort2(id_bytes[i]);
						byteswritten+=2;
					}
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						WriteFilterOutputPort2(data_bytes[i]);
						byteswritten+=2;
					}
				} // if
				/**
				 * In the case of altitude, we only need to send the data to
				 * AltConverterA
				 */
				else if (id == 2) {
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(id_bytes[i]);
						byteswritten++;
					}
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						byteswritten++;
					}			 
				} // else if
				/**
				 * Where in the case of temperature, it is sufficient to go to
				 * TempConverterA
				 */
				else if (id == 4) {
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort2(id_bytes[i]);
						byteswritten++;
					}
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort2(data_bytes[i]);
						byteswritten++;
					}
				} // else if
			} // try

			//Catch the eos exception
			catch (EndOfStreamException e) {
				ClosePorts();
				ClosePorts2();
				System.out.print( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				break;

			} // catch

		} // while

	} // run
	
	/**
	 * This method DOES NOT override the superclass write method. Both this
	 * method and the super class method are used. This method simply
	 * writes the data to the second output port, defined in the beginning.
	 * @param datum
	 */
	void WriteFilterOutputPort2(byte datum)
	{
		try
		{
            OutputWritePort2.write((int) datum );
		   	OutputWritePort2.flush();

		} // try

		catch( Exception Error )
		{
			System.out.println("\n" + this.getName() + " Pipe write error::" + Error );

		} // catch

		return;

	} // WriteFilterPort2
	
	/**
	 * This method DOES NOT override the superclass method for closing ports.
	 * Where the super class method closes input port and the first output port,
	 * this method closes the second output port.
	 */
	void ClosePorts2() {
		try {
			OutputWritePort2.close();
		}
		catch( Exception Error ) {
			System.out.println( "\n" + this.getName() + " ClosePorts error::" + Error );

		} // catch

	} // ClosePorts

	public PipedOutputStream getOutputWritePort2() {
		return OutputWritePort2;
	}
	public PipedOutputStream getOutputWritePort3() {
		return OutputWritePort3;
	}
} // MiddleFilter