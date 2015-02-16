package SystemC;

/***************************************************************************
* DataFreameFilterFramework extend FilterFramework to encapsulate some 
* function utilities to read data frame from streams.
****************************************************************************/
public class DataFrameFilterFrameworkC extends FilterFrameworkC 
{
	
	/***************************************************************************
	* Read int from inputport
	* @throws EndOfStreamException 
	****************************************************************************/
	private int readIntFromInputPort(int portId) throws EndOfStreamException
	{
		int IdLength = 4;				// This is the length of IDs in the byte stream
		byte databyte = 0;				// This is the data byte read from the stream

		int id = 0;							// This is the measurement id
		for (int i=0; i<IdLength; i++ )
		{
			databyte = ReadFilterInputPort(portId);	// This is where we read the byte from the stream...

			id = id | (databyte & 0xFF);		// We append the byte on to ID...

			if (i != IdLength-1)				// If this is not the last byte, then slide the
			{									// previously appended byte to the left by one byte
				id = id << 8;					// to make room for the next byte we append to the ID

			} // if
		} // for
		return id;
	}
	
	/***************************************************************************
	 * Read Long from inputport
	 * @throws EndOfStreamException 
	****************************************************************************/
	private long readLongFromInputPort(int portId) throws EndOfStreamException
	{
		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		byte databyte = 0;				// This is the data byte read from the stream
		long measurement = 0;

		for (int i = 0; i < MeasurementLength; i++ )
		{
			databyte = ReadFilterInputPort(portId);
			measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...

			if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
			{												// previously appended byte to the left by one byte
				measurement = measurement << 8;				// to make room for the next byte we append to the
															// measurement
			} // if
		} // if
		return measurement;
	}
	
	/***************************************************************************
	 * Writ Int to OutputPort
	 * @throws EndOfStreamException 
	****************************************************************************/
	private int writeIntToOutputPort(int v) throws EndOfStreamException
	{
		WriteFilterOutputPort((byte)((v >>> 24) & 0xFF));
		WriteFilterOutputPort((byte)((v >>> 16) & 0xFF));
		WriteFilterOutputPort((byte)((v >>> 8) & 0xFF));
		WriteFilterOutputPort((byte)((v >>> 0) & 0xFF));
		return 4;
	}
	
	/***************************************************************************
	 * Write Measurement from inputport
	 * @throws EndOfStreamException 
	****************************************************************************/
	private int writeLongToOutputPort(long v) throws EndOfStreamException
	{
		writeIntToOutputPort((int)((v >>> 32) & 0xFFFFFFFF));
		writeIntToOutputPort((int)((v >>> 0) & 0xFFFFFFFF));
		return 8;
	}
	
	/***************************************************************************
	 * Read a dataframe from InputPort
	 * @throws EndOfStreamException 
	****************************************************************************/
	DataFrameC readDataFrame(int portId)
	{
		DataFrameC df = new DataFrameC(); // This is the data frame from the stream

		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		
		/***************************************************************************
		// iterate to get all the measurements of a data frame from InputPort
		****************************************************************************/
		for (int i= 0;i < 6; i++)
		{
			
			try {
				id = readIntFromInputPort(portId);
				measurement = readLongFromInputPort(portId);
			} catch (EndOfStreamException e) {
				return null;
			}
			
			/****************************************************************************
			// If the id = 0 then this is a time value and is therefore a long value . 
			// However, if the id is something other than 0, then the bits in the
			// long value is really of type double and we need to convert the value using
			// Double.longBitsToDouble(long val) to do the conversion which is illustrated.
			// below.
			****************************************************************************/
			switch (id) {
			case 0:
				df.timestamp = measurement;
				break;
			case 1:
				df.velocity = Double.longBitsToDouble(measurement);
				break;
			case 2:
				df.altitude = Double.longBitsToDouble(measurement);
				break;
			case 3:
				df.pressure = Double.longBitsToDouble(measurement);
				break;
			case 4:
				df.temperature = Double.longBitsToDouble(measurement);
				break;
			case 5:
				df.attitude = Double.longBitsToDouble(measurement);
				break;
			}

		} 
		return df;
	}
	
	/***************************************************************************
	 * Write a dataframe to OutputPort
	 * @throws EndOfStreamException 
	****************************************************************************/
	public int writeDataFrame(DataFrameC df) throws EndOfStreamException {
		int byteswrite = 0;
		// write timestamp to OutputPort
		byteswrite +=  writeIntToOutputPort(0);	
		byteswrite +=  writeLongToOutputPort(df.timestamp);
		
		// write velocity to OutputPort
		byteswrite +=  writeIntToOutputPort(1);
		byteswrite +=  writeLongToOutputPort(Double.doubleToLongBits(df.velocity));
		
		// write altitude to OutputPort
		byteswrite +=  writeIntToOutputPort(2);
		byteswrite +=  writeLongToOutputPort(Double.doubleToLongBits(df.altitude));
		
		// write pressure to OutputPort
		byteswrite +=  writeIntToOutputPort(3);
		byteswrite +=  writeLongToOutputPort(Double.doubleToLongBits(df.pressure));
		
		// write temperature to OutputPort
		byteswrite +=  writeIntToOutputPort(4);
		byteswrite +=  writeLongToOutputPort(Double.doubleToLongBits(df.temperature));
		
		// write attitude to OutputPort
		byteswrite +=  writeIntToOutputPort(5);
		byteswrite +=  writeLongToOutputPort(Double.doubleToLongBits(df.attitude));

		return byteswrite;
	}
}
