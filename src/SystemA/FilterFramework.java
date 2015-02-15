package SystemA;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import javax.xml.crypto.dsig.spec.XPathType.Filter;

/**
* 
* @author ligu
* Description:
*
* This superclass defines a skeletal filter framework that defines a filter in terms of the input and output
* ports. All filters must be defined in terms of this framework - that is, filters must extend this class
* in order to be considered valid system filters. Filters as standalone threads until the inputport no longer
* has any data - at which point the filter finishes up any work it has to do and then terminates.
*
* Parameters:
*
* InputReadPort:	This is the filter's input port. Essentially this port is connected to another filter's piped
*					output steam. All filters connect to other filters by connecting their input ports to other
*					filter's output ports. This is handled by the Connect() method.
*
* OutputWritePort:	This the filter's output port. Essentially the filter's job is to read data from the input port,
*					perform some operation on the data, then write the transformed data on the output port.
*
* FilterFramework:  This is a reference to the filter that is connected to the instance filter's input port. This
*					reference is to determine when the upstream filter has stopped sending data along the pipe.
*
* convertedCodes:   This is a set of integers that represents what kind of inputs the filter is allowed to take.
* 					This is a restriction that we took on when we used AcmeStudio as the architecture builder
* 					since every pipe in AcmeStudio must be coded. 
*
* Internal Methods:
*
*	public void Connect( FilterFramework Filter )
*	public byte ReadFilterInputPort()
*	public void WriteFilterOutputPort(byte datum)
*	public boolean EndOfInputStream()
*   protected void setInputType(HashSet<Integer> Codes)
*   protected void validateConvertedCodes(HashSet<Integer> Codes)
*
*/
public class FilterFramework extends Thread
{
	//Input types
	private HashSet<Integer> convertedCodes= new HashSet<Integer>();
	
	// Define filter input and output ports
	private PipedInputStream InputReadPort = new PipedInputStream();
	private PipedOutputStream OutputWritePort = new PipedOutputStream();

	// The following reference to a filter is used because java pipes are able to reliably
	// detect broken pipes on the input port of the filter. This variable will point to
	// the previous filter in the network and when it dies, we know that it has closed its
	// output pipe and will send no more data.

	private FilterFramework InputFilter;
	
	/**
	 * Default constructor
	 */
	public FilterFramework() {
	}

	/**
	 * Constructor that instantiates a subclass filter with a set of input codes
	 * @param Codes
	 */
	public FilterFramework(HashSet<Integer> Codes){
		//this.convertedCodes=Codes;
		setInputType(Codes);
	}
	
	/**
	 * The method used to set the object's input codes
	 * @param Codes
	 */
	protected void setInputType(HashSet<Integer> Codes){
		for (int i : Codes) {
			this.convertedCodes.add(i);
		}
	}
	
	/**
	 * The method that validates the codes provided with the object's input codes.
	 * Every filter being connected to is passed a set of input codes that
	 * are then validated using this method. If there are discrepancies, the method
	 * throws an exception. This is similar to how AcmeStudios would raise
	 * an error if the codes do not match for filter pipes.
	 * @param Codes
	 * @throws IOException
	 */
	protected void validateConvertedCodes(HashSet<Integer> Codes) throws IOException{
		if (!(Codes.equals(this.convertedCodes))){
			throw new IOException();
		}
	}

	/**
	 * InnerClass: EndOfStreamException
	 * @author Tony
	 *
	 */
	class EndOfStreamException extends Exception {

                static final long serialVersionUID = 0; // the version for streaming

		EndOfStreamException () { super(); }

		EndOfStreamException(String s) { super(s); }

	} // class

	/**
	 * This method connects filters to each other. All connections are
	 * through the inputport of each filter. That is each filter's inputport is
	 * connected to another filter's output port through this method.
	 * This method has been modified from the original version to take a set
	 * of input codes so they can be verified against the filter object's codes.
	 * @param Filter
	 * @param codes
	 * @throws IOException
	 */
	void Connect( FilterFramework Filter, HashSet<Integer> codes ) throws IOException
	{
		Filter.validateConvertedCodes(codes);
		try
		{
			// Connect this filter's input to the upstream pipe's output stream

			InputReadPort.connect( Filter.OutputWritePort );
			setInputFilter(Filter);

		} // try

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " FilterFramework error connecting::"+ Error );

		} // catch

	} // Connect

	/**
	 * This method reads data from the input port one byte at a time.
	 * @return
	 * @throws EndOfStreamException
	 */
	byte ReadFilterInputPort() throws EndOfStreamException
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
			while (InputReadPort.available()==0 )
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
			datum = (byte)InputReadPort.read();
			return datum;

		} // try

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " Pipe read error::" + Error );
			return datum;

		} // catch

	} // ReadFilterPort

	/**
	 * This method writes data to the output port one byte at a time.
	 * @param datum
	 */
	void WriteFilterOutputPort(byte datum)
	{
		try
		{
            OutputWritePort.write((int) datum );
		   	OutputWritePort.flush();

		} // try

		catch( Exception Error )
		{
			System.out.println("\n" + this.getName() + " Pipe write error::" + Error );

		} // catch

		return;

	} // WriteFilterPort

	/**
	 * This method is used within this framework which is why it is private
	 * It returns a true when there is no more data to read on the input port of
	 * the instance filter. What it really does is to check if the upstream filter
	 * is still alive. This is done because Java does not reliably handle broken
	 * input pipes and will often continue to read (junk) from a broken input pipe.
	 * @return
	 */
	protected boolean EndOfInputStream()
	{
		if (InputFilter.isAlive())
		{
			return false;

		} else {

			return true;

		} // if

	} // EndOfInputStream

	/**
	 * This method is used to close the input and output ports of the
	 * filter. It is important that filters close their ports before the filter
	 * thread exits.
	 */
	void ClosePorts()
	{
		try
		{
			InputReadPort.close();
			OutputWritePort.close();

		}
		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " ClosePorts error::" + Error );

		} // catch

	} // ClosePorts

	/**
	 * This is actually an abstract method defined by Thread. It is called
	 * when the thread is started by calling the Thread.start() method. In this
	 * case, the run() method should be overridden by the filter programmer using
	 * this framework superclass
	 */
	public void run()
    {
		// The run method should be overridden by the subordinate class. Please
		// see the example applications provided for more details.

	} // run
	public PipedInputStream getInputReadPort() {
		return InputReadPort;
	}
	public FilterFramework getInputFilter() {
		return InputFilter;
	}
	public void setInputFilter(FilterFramework inputFilter) {
		InputFilter = inputFilter;
	}
	public PipedOutputStream getOutputWritePort() {
		return OutputWritePort;
	}


} // FilterFramework class
