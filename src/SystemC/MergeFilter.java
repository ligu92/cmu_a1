package SystemC;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MergeFilter extends DataFrameFilterFramework 
{
	/***************************************************************************
	* CONCRETE METHOD:: run
	* Purpose: merge two sorted streams.
	*
	* Arguments: void
	*
	* Returns: void
	*
	* Exceptions: IOExecption
	*
	****************************************************************************/
/*	public void run()
    {
		// read data frame from the first Stream 
		DataFrame df1 = readDataFrame(0);
		
		// read data frame from the second Stream 
		DataFrame df2 = readDataFrame(1);

		//int count = 1;
		// merge two streams sorted by timestamp. If timestamps in two streams aren't increasing, we should
		// read all two streams into a List and sort it by time. 
		while (df1 != null || df2 != null) {
			//System.out.println(count);
			//System.out.println(df1);
			//System.out.println(df2);
			try {
				if (df1 != null || df2 == null) {
					if (df1 != null) {
						writeDataFrame(df1);
						df1 = readDataFrame(0);
					}
					if (df2 != null) {
						writeDataFrame(df2);
						df2 = readDataFrame(1);
					}
				} else {
					if (df1.timestamp < df2.timestamp) {
						writeDataFrame(df1);
						df1 = readDataFrame(0);
					} else {
						writeDataFrame(df2);
						df2 = readDataFrame(1);
					}
				}
			} catch (EndOfStreamException e) {
				e.printStackTrace();
				break;
			}
		}

	} // run
*/	
	/**
	 * Sort and merge two streams
	 */
	
	
	
	public void run()
    {
		List<DataFrame> dataFrames = new ArrayList<DataFrame>();
		// read data frame from the first Stream 
		DataFrame df = readDataFrame(0);
		while (df != null) {
			dataFrames.add(df);
			df = readDataFrame(0);
		}
		// read data frame from the second Stream 
		df = readDataFrame(1);
		while (df != null) {
			dataFrames.add(df);
			df = readDataFrame(1);
		}
		Collections.sort(dataFrames, new Comparator<DataFrame>() {

			public int compare(DataFrame arg0, DataFrame arg1) {
				return (int) (arg0.timestamp - arg1.timestamp);
			}
			
		});
		
		for (DataFrame df1 : dataFrames) {
			//System.out.println(df1);
			try {
				writeDataFrame(df1);
			} catch (EndOfStreamException e) {
				e.printStackTrace();
				break;
			}
		}

	} // run
	
}
