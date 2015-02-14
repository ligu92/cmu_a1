package SystemA;

import java.util.*;

public class PlumberA {

	public static void main( String argv[])
	   {
			/****************************************************************************
			* Here we instantiate three filters.
			****************************************************************************/

			SourceFilterA source = new SourceFilterA();
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

			sink.Connect(merge);
			merge.Connect(alt);
			merge.Connect(temp);
			alt.Connect(split);
			temp.Connect(split);
			split.Connect(source);
			
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
