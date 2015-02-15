package SystemA;

import java.nio.ByteBuffer;
import java.util.HashSet;

/**
 * This is one of the converter filters for SystemA, it takes altitude
 * data and converts from feet to meters, writing the data to the downstream.
 * @author ligu
 *
 */
public class AltConverterA extends FilterFramework {
	/**
	 * Overload the super class constructor
	 * @param codesSet
	 */
	public AltConverterA(HashSet<Integer> codesSet) {
		setInputType(codesSet);
	}

	/**
	 * Starts off the filter operation
	 */
	public void run() {

		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream

		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter
		
		//Bytes arrays for buffering
		byte[] id_bytes;
		byte[] data_bytes;
		
		//When we read the altitude, we save it
		//into variable so we can convert as specified.
		double altitude = 0.0;
		int byteswritten = 0;				// Number of bytes written to the stream.

		// Next we write a message to the terminal to let the world know we are alive...

		System.out.print( "\n" + this.getName() + "::Middle Reading ");

		while (true) {
			/**
			 * Similar to other filters, we read the 4 bytes of ID.
			 */
			try {
				id = 0;
				id_bytes = new byte[4];
				for (i=0; i<IdLength; i++ ){
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...

					id = id | (databyte & 0xFF);		// We append the byte on to ID...

					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID

					} // if
					//Save into byte array for buffering
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
				for (i=0; i<MeasurementLength; i++ ) {
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...

					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
																	// measurement
					} // if
					//Buffer into array
					data_bytes[i] = databyte;
					bytesread++;									// Increment the byte count
				} // for

				/**
				 * If the id is 0, we have time data to write to the output
				 */
				if ( id == 0 ) {
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(id_bytes[i]);
						byteswritten++;
					}
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						byteswritten++;
					}
				} // if
				/**
				 * If the id is 2, we have altitude data. 
				 * We can simply convert the double from feet to meters
				 * Then we fill the measurements byte array with 8 bytes
				 * of the altitude in meters so we can write downstream.
				 */
				if (id == 2) {
					altitude = Double.longBitsToDouble(measurement);
					altitude *= 0.3048;
					ByteBuffer.wrap(data_bytes).putDouble(altitude);
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(id_bytes[i]);
						byteswritten++;
					}
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						byteswritten++;
					}			 
				} // else if
			} // try

			catch (EndOfStreamException e) {
				ClosePorts();
				System.out.print( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				break;

			} // catch

		} // while

   } // run

} // AltConverterA