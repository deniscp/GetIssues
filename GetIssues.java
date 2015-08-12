import java.io.*;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class GetIssues
{
    private String url;
    private GregorianCalendar date;
    private int numIssues;
    private PreparedStatement pstmt;
    private Connection dbCon;
    
    GetIssues(String url,GregorianCalendar date){
	this.url=url;
	this.date=date;
	this.numIssues=0;
	this.pstmt=null;
	this.dbCon=null;
    }

    String get(String targetURL)
    {
	String response=null;

	HttpsURLConnection connection = null;  
	try {
	    //Create connection
	    URL url = new URL(targetURL);
	    connection = (HttpsURLConnection)url.openConnection();
	    connection.setRequestMethod("GET");
	    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
	    connection.setRequestProperty("User-Agent", "Firefox");
	    connection.setUseCaches(false);



	    System.out.println("\nSending 'GET' request to URL : " + url);
	    connection.connect();
	    int responseCode = connection.getResponseCode();
	    System.out.println("getRequestProperty(\"User-Agent\"): "+connection.getRequestProperty("User-Agent"));
	    System.out.println("getRequestProperty(\"Host\"): "+connection.getRequestProperty("Host"));
	    System.out.println("getRequestProperty(\"Accept\"): "+connection.getRequestProperty("Accept"));
	    System.out.println("getRequestProperty(\"Connection\"): "+connection.getRequestProperty("Connection"));
	    System.out.println("getRequestProperty(\"Cache-Control\"): "+connection.getRequestProperty("cache-control"));
	    System.out.println("Response Code : " + responseCode);

 
	    
	    System.out.println("\nPrinting Response Header...\n");
	    Map<String, List<String>> map = connection.getHeaderFields();
	    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
	        System.out.println( entry.getKey() + ": " + connection.getHeaderField(entry.getKey()) );
	    }
	    System.out.println("------------------------------\n");
	   


	    //Get Response  
	    InputStream is = connection.getInputStream();
	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	    StringBuilder strBld = new StringBuilder(); // or StringBuffer if not Java 5+ 

	    String line;
	    int numLine=0;
	    while((line = rd.readLine()) != null) {

		strBld.append(line);
		strBld.append('\n');
		numLine++;
	    }
	    rd.close();
	    is.close();


	    response= strBld.toString();

	}
	catch (SocketTimeoutException toe)
	{
	    toe.printStackTrace();
	    System.out.println("Timeout expired before the connection can be established!\nHave been transferred "+toe.bytesTransferred+" bytes.");
	    toe.getMessage();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
	finally {
	    if(connection != null) {
		connection.disconnect(); 
	    }
	}
	return response;
    }

    String[] fromListToArray(String response)
    {	
	String[] repos;	
	int i=0;
	int level=0;
	boolean begin,end;
	int numEl=0;
	final String body="\"body\":";

	//Scorro l'intera risposta per contare quanti elementi
	//appartegono al vettore
	for(i=0;i<response.length();i++)
	{
	    //Ignoro il contenuto del campo "body":
	    if( (response.length()-i) > body.length() )
		if( response.substring(i,i+body.length()).equals(body) )
		{
		    for(i=i+body.length(); response.charAt(i)!='"' ; i++);
		    i++;
		    for( ; response.charAt(i)!='"' || response.charAt(i-1)=='\\' ; i++);
		    i++;
		}


	    if(level==0)
		begin=true;
	    else
		begin=false;

	    if(response.charAt(i)=='{')
	    {
		level++;
	    }
	    else if(response.charAt(i)=='}')
	    {
		level--;
	    }

	    if(begin && level==1)
		numEl++;
	}


	repos=new String[numEl];

	numEl=0;
	level=0;
	int beginIndex=0,endIndex=0;

	//Scorro l'intera risposta per prelevare gli elementi del
	//vettore da assegnare a repos[]
	for(i=0;i<response.length();i++)
	{
	    //Ignoro il contenuto del campo "body":
	    if( (response.length()-i) > body.length() )
		if( response.substring(i,i+body.length()).equals(body) )
		{
		    for(i=i+body.length(); response.charAt(i)!='"' ; i++);
		    i++;
		    for( ; response.charAt(i)!='"' || response.charAt(i-1)=='\\' ; i++);
		    i++;
		}

	    if(level==0)
	    {
		begin=true;
	    }
	    else
		begin=false;

	    if(level==1)
		end=true;
	    else
		end=false;


	    if(response.charAt(i)=='{')
	    {
		level++;
	    }
	    else if(response.charAt(i)=='}')
	    {
		level--;
	    }

	    if(begin && level==1)
	    {
		beginIndex=i+1;
		numEl++;
	    }

	    if(end && level==0)
	    {
		endIndex=i;
		repos[numEl-1]=response.substring(beginIndex,endIndex);
	    }

	}

	return repos;
    }

    void getIssues(String[] repos)
    {
	int i;
	final String has_issues="\"has_issues\":";
	final String full_name="\"full_name\":";
	final String issues_url="\"issues_url\":";
	String value;
	String repoName;
	String url;
	int beginIndex,endIndex;

	for(i=0;i<repos.length;i++)
	{
	    //Leggo il campo "has_issues": del repository[i]
	    beginIndex=repos[i].indexOf(has_issues)+has_issues.length();
	    for( ; repos[i].charAt(beginIndex)==' ' ; beginIndex++);
	    endIndex=beginIndex+"true".length();
	    value=repos[i].substring( beginIndex, endIndex );

	    //Leggo il campo "full_name": del repository[i]
	    beginIndex=repos[i].indexOf(full_name)+full_name.length();
	    for( ; repos[i].charAt(beginIndex)!='"';beginIndex++);
	    beginIndex++;
	    for( endIndex=beginIndex; repos[i].charAt(endIndex)!='"';endIndex++);
	    repoName=repos[i].substring( beginIndex, endIndex );


	    if( value.equals("true") )
	    {
		//se "has_issues":true leggo il campo "issues_url":
		beginIndex=repos[i].indexOf(issues_url)+issues_url.length();
		for( ; repos[i].charAt(beginIndex)!='"' ; beginIndex++);
		beginIndex++;
		for(endIndex=beginIndex;repos[i].charAt(endIndex)!='"' && repos[i].charAt(endIndex)!='{';endIndex++);
		url=repos[i].substring(beginIndex,endIndex);

		//stampo su files gli issues del repository[i]
		printIssues(repoName,url);
	    }
	    else
		System.out.println("Il repository \""+repoName+"\" non ha issues associati.");
	}

	try{
	    if(pstmt!=null){
		pstmt.close(); pstmt=null;}

	    if(dbCon!=null){
		dbCon.close(); dbCon=null;}
	}
	catch(SQLException ex){
	    System.err.println("SQLException: "+ex.getMessage());
	}
    }

    void printIssues(String repoName, String url)
    {
	SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat sdf2 =new SimpleDateFormat("dd-MM-yyy");
	GregorianCalendar date=new GregorianCalendar();
	int i,j;
	final String state="\"state\":";
	final String created_at="\"created_at\":";
	final String number="\"number\":";
	String state_value,created_at_value,number_value;
	String issueName;
	int beginIndex,endIndex;
	File dir=null;
	PrintWriter file=null;


	System.out.println("Name: "+repoName+", url: "+url);
	String response=get(url);
	String[] issues=fromListToArray(response);

	
	for(i=0,j=0;i<issues.length;i++)
	{
	    //Leggo il campo "state": dell' issue[i]
	    beginIndex=issues[i].indexOf(state)+state.length();
	    for( ; issues[i].charAt(beginIndex)!='"' ; beginIndex++);
	    beginIndex++;
	    endIndex=beginIndex+"open".length();
	    state_value=issues[i].substring( beginIndex, endIndex );

	    //Leggo il campo "number": dell' issue[i]
	    beginIndex=issues[i].indexOf(number)+number.length();
	    for( ; issues[i].charAt(beginIndex)==' ' ; beginIndex++);
	    for(endIndex=beginIndex;issues[i].charAt(endIndex)!=',';endIndex++);
	    number_value=issues[i].substring( beginIndex, endIndex );

	    //Leggo il campo "created_at": dell' issue[i]
	    beginIndex=issues[i].indexOf(created_at)+created_at.length();
	    for( ; issues[i].charAt(beginIndex)!='"' ; beginIndex++);
	    beginIndex++;
	    for(endIndex=beginIndex;issues[i].charAt(endIndex)!='T';endIndex++);
	    created_at_value=issues[i].substring( beginIndex, endIndex );

	    try{
		date.setTime(sdf.parse(created_at_value));
	    }
	    catch(ParseException e){
		e.printStackTrace();
	    }

	    issueName=repoName.replace('/','-') + " - " + number_value;
	    System.out.println("\n Nome Issue: "+issueName+"\n \"state\": "+state_value);
	    System.out.println(" \"created_at\": "+sdf2.format(date.getTime()) +" | input date: "+ sdf2.format(this.date.getTime()));

	    if(state_value.equals("open") && this.date.compareTo(date)<=0)
	    {
		j++;
		numIssues++;

		if(numIssues==1)
		{
		    openDB();
		    try{
			pstmt=dbCon.prepareStatement("INSERT INTO Issues "+
				"VALUES (?,?,?,?,?)");
		    }
		    catch(SQLException ex){
			System.err.println("SQLException: "+ex.getMessage()+"\nDatabase access error occurred");
			System.exit(1);
		    }
		}

		try{
		    pstmt.setString(1,repoName);
		    pstmt.setInt(2,Integer.parseInt(number_value));
		    pstmt.setDate(3,new java.sql.Date( date.getTime().getTime() ));
		    pstmt.setString(4,state_value);
		    pstmt.setString(5,issues[i]);
		    pstmt.executeUpdate();
		}
		catch(SQLException ex){
		    System.err.println("SQLException: "+ex.getMessage()+"\nErrore durante l'inserimento dei dati");
		}


		if(j==1)
		{
		    dir=new File(repoName);
		    if( dir.isDirectory())
		    {
			for (File c : dir.listFiles())
			    c.delete();
		    }
		    else
			dir.mkdirs();
		}

		try{
		    file=new PrintWriter(repoName+"/"+issueName);
		}
		catch(Exception e){
		    e.printStackTrace();
		}
		file.write(issues[i]);
		file.flush();
		file.close();

		System.out.println(issues[i]+"\n----------------------------------------");
	    }
	}
	System.out.println("\n Issues aperti e antecedenti alla data passata in input: "+j+'/'+issues.length+"\n--------------------------------------\n\n");
    }

    void openDB()
    {
	String username;
	String password;
	String dbUrl;
	Statement stmt=null;

	username="postgres";
	password="postgres";
	dbUrl="jdbc:postgresql://localhost:5432/GetIssues";
	
	try{
	    //Required to manually load any drivers prior to JDBC 4.0
	    //Class.forName("org.postgresql.Driver");
	    dbCon=DriverManager.getConnection(dbUrl,username,password);
	    stmt = dbCon.createStatement();
	    stmt.executeUpdate("DROP TABLE IF EXISTS Issues");
	    stmt.executeUpdate("CREATE TABLE Issues" +
		    "(Full_Name varchar(50) NOT NULL, " +
		    "Number integer NOT NULL, " +
		    "Created_at date NOT NULL, " +
		    "State varchar(5) NOT NULL, " +
		    "Issue text NOT NULL, " +
		    "PRIMARY KEY (Full_Name,Number))");
	}
	/*
	catch(ClassNotFoundException ex){
	    System.err.println("ClassNotFoundException: "+ex.getMessage());
	    System.err.println("Driver jdbc non trovato");
	    System.exit(1);
	}
	*/
	catch(SQLException ex){
	    System.err.println("SQLException: "+ex.getMessage());
	    System.err.println("Accesso al database non riuscito");
	    System.exit(1);
	}
	finally{
	    if(stmt!=null)
		try{stmt.close();}
	    	catch(SQLException ex)
	    	{
		    System.err.println("SQLException: "+ex.getMessage());
		}
	}

	System.err.println("Accesso al DB effettuato correttamente e tabella Issues creata");
    }
}//End of Class GetIssues
