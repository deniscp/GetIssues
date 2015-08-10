import java.io.*;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.text.ParseException;


public class Test{
    public static void main(String args[])
    {
	String targetURL=null;
	String data=null;

	if(args.length != 2)
	{
	    System.out.println("Test https://url dd-MM-yyyy");
	    return;
	}

	targetURL=args[0];
	data=args[1];

	//targetURL="https://api.github.com/users/deniscp/repos";
	//targetURL="https://api.github.com/orgs/octokit/repos";
	//data="01-07-2015";
	
	/*
	targetURL="https://api.github.com/repos/octokit/go-octokit";
	targetURL="https://api.github.com/repos/octokit/octokit.rb";
	targetURL="https://api.github.com/repos/octokit/octokit.objc";
	targetURL="https://api.github.com/repos/octokit/octokit.net";
	targetURL="http://localhost/nuovo/issues3";
	targetURL="https://api.github.com/repos/octokit/octokit.net/issues?page=1";
	*/

	SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
	GregorianCalendar date=new GregorianCalendar();
	try{
	    date.setTime(sdf.parse(data));
	}
	catch (ParseException e){
	    e.printStackTrace();
	}

	GetIssues conn=new GetIssues(targetURL,date);
	int i;
	int j;

	String response=conn.get(targetURL);
	String[] repos=conn.fromListToArray(response);


	conn.getIssues(repos);	


	/*
	int openedCurl=0,closedCurl=0;
	for(i=0;i<response.length();i++)
	{
	    if(response.charAt(i)=='{')
		openedCurl++;
	    else if(response.charAt(i)=='}')
		closedCurl++;
	}

	System.out.println("OpenedCurl: "+openedCurl+"; ClosedCurl: "+closedCurl);
	*/


    }//end of main
}//end of Test class


