package SystemA;

import java.io.IOException;
import java.util.*;

/**
 * This class is the Plumber for SystemA. It instantiates all the filter subclasses
 * needed for the first System, each with a set of input codes as defined by the
 * provided architecture. It then connects all the filters and starts them off.
 * @author ligu
 *
 */
public class PlumberA {

	/**
	 * The main method is ran, it starts all the filters off.
	 * @param argv
	 * @throws IOException
	 */
	public static void main( String argv[]) throws IOException
	   {
			/****************************************************************************
			* Here we instantiate filters:
			* source: the filter that reads the input file
			* split:  the filter that splits up the inputstream and sends relevant
			*         data to the downstream filters
			* alt:	  the filter that converts altitude to metric
			* temp:   the filter that converts F to C for temperature
			* merge:  the filter that takes the timestams, altitudes, and temperatures and
			* 		  combines them.
			* sink:	  the final filter that writes the output to a file	 
			****************************************************************************/

			SourceFilterA source = new SourceFilterA(new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			SplitterFilterA split = new SplitterFilterA(new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			AltConverterA alt = new AltConverterA(new HashSet<Integer>(Arrays.asList(0,2)));
			TempConverterA temp = new TempConverterA(new HashSet<Integer>(Arrays.asList(0,4)));
			MergeFilterA merge = new MergeFilterA(new HashSet<Integer>(Arrays.asList(0,2,4)));
			SinkFilterA sink = new SinkFilterA();

			// Connects all the filters, see SystemA.acme for connection details
			sink.Connect(merge, new HashSet<Integer>(Arrays.asList(0,2,4)));
			merge.Connect(alt, new HashSet<Integer>(Arrays.asList(0,2)));
			merge.Connect(temp, new HashSet<Integer>(Arrays.asList(0,4)));
			alt.Connect(split, new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			temp.Connect(split, new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			split.Connect(source, new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5)));
			

			// Start the filters, like Tony said in the original, not very exciting
			source.start();
			split.start();
			alt.start();
			temp.start();
			sink.start();
			merge.start();

	   } // main
} // PlumberA
