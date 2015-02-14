package SystemA;

import java.io.IOException;
import java.util.*;

public class PlumberA {

	public static void main( String argv[]) throws IOException
	   {
			/****************************************************************************
			* Here we instantiate three filters.
			****************************************************************************/

			SourceFilterA source = new SourceFilterA(new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			SplitterFilterA split = new SplitterFilterA(new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			AltConverterA alt = new AltConverterA(new HashSet<Integer>(Arrays.asList(0,2)));
			TempConverterA temp = new TempConverterA(new HashSet<Integer>(Arrays.asList(0,4)));
			MergeFilterA merge = new MergeFilterA(new HashSet<Integer>(Arrays.asList(0,2,4)));
			SinkFilterA sink = new SinkFilterA();

			/****************************************************************************
			* Here we connect the filters starting with the sink filter (Filter 1) which
			* we connect to Filter2 the middle filter. Then we connect Filter2 to the
			* source filter (Filter3).
			****************************************************************************/

			sink.Connect(merge, new HashSet<Integer>(Arrays.asList(0,2,4)));
			merge.Connect(alt, new HashSet<Integer>(Arrays.asList(0,2)));
			merge.Connect(temp, new HashSet<Integer>(Arrays.asList(0,4)));
			alt.Connect(split, new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			temp.Connect(split, new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			split.Connect(source, new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			
			//sink.Connect(source, new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			
			/****************************************************************************
			* Here we start the filters up. All-in-all,... its really kind of boring.
			****************************************************************************/

			source.start();
			split.start();
			alt.start();
			temp.start();
			sink.start();
			merge.start();

	   } // main

}
