package SystemA;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;

import SystemA.FilterFramework.EndOfStreamException;

/******************************************************************************************************************
* File:MiddleFilter.java
* Course: 17655
* Project: Assignment 1
* Copyright: Copyright (c) 2003 Carnegie Mellon University
* Versions:
*	1.0 November 2008 - Sample Pipe and Filter code (ajl).
*
* Description:
*
* This class serves as an example for how to use the FilterRemplate to create a standard filter. This particular
* example is a simple "pass-through" filter that reads data from the filter's input port and writes data out the
* filter's output port.
*
* Parameters: 		None
*
* Internal Methods: None
*
******************************************************************************************************************/

public class MergeFilterA extends FilterFramework {
	private PipedInputStream InputReadPort2 = new PipedInputStream();
	
	public MergeFilterA(HashSet<Integer> codesSet) {
		setInputType(codesSet);
	}

	public void run() {
		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream

		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		long measurement2;
		int id;							// This is the measurement id
		int id2;
		int i;							// This is a loop counter
		
		byte[] id_bytes;
		byte[] data_bytes;
		byte[] id_bytes2;
		byte[] data_bytes2;

		int byteswritten = 0;				// Number of bytes written to the stream.

		// Next we write a message to the terminal to let the world know we are alive...

		System.out.print( "\n" + this.getName() + "::Middle Reading ");

		while (true) {
			try {				
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

				measurement2 = 0;
				data_bytes2 = new byte[8];
				for (i=0; i<MeasurementLength; i++ )
				{
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
					data_bytes[i] = databyte;
					bytesread++;									// Increment the byte count

				} // if
				
				
				/*if ( id == 0 )
				{
					Calendar TimeStamp = Calendar.getInstance();
					SimpleDateFormat TimeStampFormat = new SimpleDateFormat("yyyy::dd:hh:mm:ss");
					TimeStamp.setTimeInMillis(measurement);
					System.out.print( 
							TimeStampFormat.format(TimeStamp.getTime()) + " ID = " + id + " " + Double.longBitsToDouble(measurement) );
				} // if*/
				
				
				for (i = 0; i < 4; i++) {
					WriteFilterOutputPort(id_bytes2[i]);
					byteswritten++;
				}
				for (i = 0; i < 8; i++) {
					WriteFilterOutputPort(data_bytes2[i]);
					byteswritten++;
				}
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
	
	byte ReadFilterInputPort2() throws EndOfStreamException
	{
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
	
	void ClosePorts2() {
		try {
			InputReadPort2.close();
		}
		catch( Exception Error ) {
			System.out.println( "\n" + this.getName() + " ClosePorts error::" + Error );

		} // catch

	} // ClosePorts2
	
	void Connect( AltConverterA Filter ) {
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
	
	void Connect( TempConverterA Filter ) {
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
} // MiddleFilter
