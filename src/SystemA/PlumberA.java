package SystemA;

import java.util.*;

public class PlumberA {

	public static void main( String argv[])
	   {
			/****************************************************************************
			* Here we instantiate three filters.
			****************************************************************************/

			SourceFilterA Filter1 = new SourceFilterA();
			SplitterFilterA Filter2 = new SplitterFilterA(new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			AltConverterA alt = new AltConverterA(new HashSet<Integer>(Arrays.asList(0,2)));
			TempConverterA temp = new TempConverterA(new HashSet<Integer>(Arrays.asList(0,4)));
			SinkFilterA Filter3 = new SinkFilterA();

			/****************************************************************************
			* Here we connect the filters starting with the sink filter (Filter 1) which
			* we connect to Filter2 the middle filter. Then we connect Filter2 to the
			* source filter (Filter3).
			****************************************************************************/

			Filter3.Connect(Filter2); // This esstially says, "connect Filter3 input port to Filter2 output port
			Filter2.Connect(Filter1); // This esstially says, "connect Filter2 intput port to Filter1 output port

			/****************************************************************************
			* Here we start the filters up. All-in-all,... its really kind of boring.
			****************************************************************************/

			Filter1.start();
			Filter2.start();
			Filter3.start();

	   } // main

}
