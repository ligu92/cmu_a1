package SystemB;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;

import SystemB.FilterFramework.EndOfStreamException;

public class SinkFilterWild extends FilterFramework  {
	/**
	 * This method runs the sink filter.
	 */
	public void run() {
		/************************************************************************************
		*	TimeStamp is used to compute time using java.util's Calendar class.
		* 	TimeStampFormat is used to format the time value so that it can be easily printed
		*	to the terminal.
		*************************************************************************************/

		Calendar TimeStamp = Calendar.getInstance();
		SimpleDateFormat TimeStampFormat = new SimpleDateFormat("yyyy::dd:hh:mm:ss");

		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream

		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter
		
		String outFileName = "WildPoints.dat"; // This is the filename of the output
		
		//When we read the altitude and the temperature, we save them
		//into variables so we can format the decimals as the writeup specified.
		double altitude = 0.0;
		DecimalFormat altFormatter = new DecimalFormat("000000.00000");
		double temperature = 0.0;
		DecimalFormat tempFormatter = new DecimalFormat("000.00000");
		double pressure = 0.0;
		DecimalFormat presFormatter = new DecimalFormat("00.00000");
		
		//Make a file writer so we can create the output.
		File file = new File(outFileName);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
		} catch (IOException e2) {
			e2.printStackTrace();
		}


		/*************************************************************
		*	First we announce to the world that we are alive...
		**************************************************************/

		System.out.print( "\n" + this.getName() + "::Sink Reading ");

		while (true) {
			try {
				/***************************************************************************
				// We know that the first data coming to this filter is going to be an ID and
				// that it is IdLength long. So we first decommutate the ID bytes.
				****************************************************************************/
				
				id = 0;
				for (i=0; i<IdLength; i++ ) {
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...

					id = id | (databyte & 0xFF);		// We append the byte on to ID...

					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID

					} // if

					bytesread++;						// Increment the byte count

				} // for

				/****************************************************************************
				// Here we read measurements. All measurement data is read as a stream of bytes
				// and stored as a long value. This permits us to do bitwise manipulation that
				// is necessary to convert the byte stream into data words. Note that bitwise
				// manipulation is not permitted on any kind of floating point types in Java.
				// If the id = 0 then this is a time value and is therefore a long value - no
				// problem. However, if the id is something other than 0, then the bits in the
				// long value is really of type double and we need to convert the value using
				// Double.longBitsToDouble(long val) to do the conversion which is illustrated.
				// below.
				*****************************************************************************/

				measurement = 0;
				for (i=0; i<MeasurementLength; i++ )
				{
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...

					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
																	// measurement
					} // if

					bytesread++;									// Increment the byte count

				} // if

				/****************************************************************************
				// Here we look for an ID of 0 which indicates this is a time measurement.
				// Every frame begins with an ID of 0, followed by a time stamp which correlates
				// to the time that each proceeding measurement was recorded. Time is stored
				// in milliseconds since Epoch. This allows us to use Java's calendar class to
				// retrieve time and also use text format classes to format the output into
				// a form humans can read. So this provides great flexibility in terms of
				// dealing with time arithmetically or for string display purposes. This is
				// illustrated below.
				****************************************************************************/

				if ( id == 0 )
				{
					TimeStamp.setTimeInMillis(measurement);
					//Write the timestamp in the human-readable format
					writer.write(TimeStampFormat.format(TimeStamp.getTime()) + "\t");

				} // if

				/****************************************************************************
				* Here we write data to the output. If the id is 2, then we have
				* an altitude, so we format it according to the writeup and
				* write it to the current line, along with a newline char.
				****************************************************************************/
				else if (id == 2)
				{
					altitude = Double.longBitsToDouble(measurement);					
					writer.write(altFormatter.format(altitude) + "\n");
					altitude = 0.0;
					 
				} // else if
				
				/****************************************************************************
				* Here we write data to the output. If the id is 4, then we have
				* an temperature, so we format it according to the writeup and
				* write it to the current line, along with a tab char.
				****************************************************************************/
				else if (id == 4) {
					temperature = Double.longBitsToDouble(measurement);
					writer.write(tempFormatter.format(temperature) + "\t");
					temperature = 0.0;
				} // else if
				else if (id == 103) {
					pressure = Double.longBitsToDouble(measurement);
					writer.write(presFormatter.format(pressure) + "\t");
					pressure = 0.0;
				} // else if


			} // try

			/*******************************************************************************
			*	The EndOfStreamExeception below is thrown when you reach end of the input
			*	stream (duh). At this point, the filter ports are closed and a message is
			*	written letting the user know what is going on.
			********************************************************************************/
			
			catch (EndOfStreamException e)
			{
				ClosePorts();
				System.out.print( "\n" + this.getName() + "::Sink Exiting; bytes read: " + bytesread );
				try {
					writer.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				break;
			} // catch
			catch (IOException e) {
				e.printStackTrace();
			}

		} // while

   } // run
	
	/**
	 * This method OVERRIDES the superclass connect method. It does not take
	 * a generic FilterFramework, rather, it takes a specific Splitter Filter.
	 * While this violates the reusability feature somewhat, it is necessary
	 * since only a Splitter filter in this system has a second output port.
	 * The default Framework does not have multiple outputports.
	 * @param Filter
	 * @param codes
	 * @throws IOException
	 */
	void Connect( MergeFilterB Filter, HashSet<Integer> codes ) throws IOException {
		Filter.validateConvertedCodes(codes);
		try {
			// Connect this filter's input to the upstream pipe's output stream
			getInputReadPort().connect( Filter.getOutputWritePort2() );
			setInputFilter(Filter);

		} // try

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " FilterFramework error connecting::"+ Error );

		} // catch

	} // Connect

}
