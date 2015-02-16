package SystemC;
/******************************************************************************************************************
* File:PlumberC.java
* Course: 17655
* Project: Assignment 1
* Copyright: Copyright (c) 2003 Carnegie Mellon University
* Versions:
*	1.0 November 2008 - Sample Pipe and Filter code (ajl).
*
* Description:
*
* This class serves as an example to illstrate how to use the PlumberTemplate to create a main thread that
* instantiates and connects a set of filters. This example consists of three filters: a source, a middle filter
* that acts as a pass-through filter (it does nothing to the data), and a sink filter which illustrates all kinds
* of useful things that you can do with the input stream of data.
*
* Parameters: 		None
*
* Internal Methods:	None
*
******************************************************************************************************************/
public class PlumberC
{
   public static void main(String argv[])
   {
		/****************************************************************************
		* Here we instantiate three filters.
		****************************************************************************/

	   	String input1 = "DataSets/SubSetA.dat";
	   	String input2 = "DataSets/SubSetB.dat";
		SourceFilterC Filter1 = new SourceFilterC(input1);
		SourceFilterC Filter2 = new SourceFilterC(input2);
		
		// the Filter to merge the two input streams
		MergeFilterC Filter3 = new MergeFilterC();
		Filter3.Connect(Filter1);
		Filter3.Connect(Filter2);
		
		AltitudeFilterC Filter4 = new AltitudeFilterC("LessThan10K.dat");
		Filter4.Connect(Filter3);

		PressureWildPointsFilterC Filter5 = new PressureWildPointsFilterC("PressureWildPoints.dat");
		Filter5.Connect(Filter4);
		
		/****************************************************************************
		* Here we start the filters up. All-in-all,... its really kind of boring.
		****************************************************************************/

		Filter1.start();
		Filter2.start();
		Filter3.start();
		Filter4.start();
		Filter5.start();

   } // main

} // Plumber