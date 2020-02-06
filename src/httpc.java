
import java.io.*;
import java.net.*;


public class httpc {
	
	private static String path = "", url = "", host = "", headers = "", data = "", dashOPath = ""; 
	private static boolean contentLength = true;
	private static boolean dashV = false, dashO = false;

	public static void main(String[] args) {
		
		//First off, if no arguments are provided, help menu will be opened.
		if (args.length == 0) {
			helpMe(args);
			System.exit(1);
		}
			
		//identifying whether this is a get or post request
		String httpMethod = args[0].toLowerCase(); 
		boolean isGetPost = httpMethod.equals("get") || httpMethod.equals("post");
		
		//After the second argument, everything else is parsed within the following block.
		if (isGetPost) {
			parseMessage(args);			
			System.out.println("Start:");
			transmitMessage(httpMethod);
		}
		
		//other than GET or POST, we can respond to a HELP request
		else if(httpMethod.equals("help")) 							
			helpMe(args);
		//defaults to help menu in all other cases, too.
		else
			helpMe(args);
	}
	
	//Method for calling the help menu.
	public static void helpMe(String[] args){
		
	String help = "httpc is a curl-like application but supports HTTP protocol only.\r\n" + 
			"Usage:\r\n" + 
			" httpc command [arguments]\r\n" + 
			"The commands are:\r\n" + 
			" get executes a HTTP GET request and prints the response.\r\n" + 
			" post executes a HTTP POST request and prints the response.\r\n" + 
			" help prints this screen.\r\n" + 
			"Use \"httpc help [command]\" for more information about a command.\r\n";
	String getHelp =  "usage: httpc get [-v] [-h key:value] URL\r\n" + 
			"Get executes a HTTP GET request for a given URL.\r\n" + 
			" -v Prints the detail of the response such as protocol, status,\r\n" + 
			"and headers.\r\n" + 
			" -h key:value Associates headers to HTTP Request with the format\r\n" + 
			"'key:value'.\r\n";
	String postHelp = "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\r\n" + 
			"Post executes a HTTP POST request for a given URL with inline data or from\r\n" + 
			"file.\r\n" + 
			" -v Prints the detail of the response such as protocol, status,\r\n" + 
			"and headers.\r\n" + 
			" -h key:value Associates headers to HTTP Request with the format\r\n" + 
			"'key:value'.\r\n" + 
			" -d string Associates an inline data to the body HTTP POST request.\r\n" + 
			" -f file Associates the content of a file to the body HTTP POST\r\n" + 
			"request.\r\n";
	if(args.length > 1){
		if(args[1].toLowerCase().equals("get"))
			System.out.println(getHelp);
		else if(args[1].toLowerCase().equals("post"))
			System.out.println(postHelp);
	}
	else
		System.out.println(help);							
	}	

	public static void parseMessage(String[] args) {
		
		BufferedReader bufread;
		
		//We'll systematically catch all the dash arguments (-v,-h,-d,-f,-o)
		for (int i = 1; i < (args.length - 1); i++) {
			
			//check for -v
			if (args[i].toLowerCase().equals("-v")) {
				dashV = true;
			}
			
			//check for -h
			if (args[i].toLowerCase().equals("-h")) {
				
				if (i+1 < (args.length -1)) {							
					i++;
					String keyval = args[i];
					
					if (keyval.toLowerCase().contains("content-length"))
						contentLength = false;
					
					String noApostropheKeyval = keyval.replace("'", "");
					headers += noApostropheKeyval + "\r\n";
				}		
			}
			
			//check for -d
			if (args[i].toLowerCase().equals("-d")) {
				
				int numberApostrophes = 0;
				while (numberApostrophes < 2) {
					if (i + 1 < args.length - 1) {
						i++;
						String argument = args[i];
						numberApostrophes += argument.length() - argument.replace("'", "").length();
						
						data += (argument.replace("'", ""));
						if (numberApostrophes < 2)
							data += " ";
					}
				}
			}
			
			//check for -f
			if (args[i].toLowerCase().equals("-f")) {
				i++;
				try {
					bufread = new BufferedReader(new FileReader(args[i]));
					StringBuilder stringBuild = new StringBuilder();
					String line = bufread.readLine();
					
					while (line != null) {
						stringBuild.append(line);
						stringBuild.append(System.lineSeparator()); //appends \n on UNIX, \r\n on Windows
						line = bufread.readLine();
					}
					
					data = stringBuild.toString().replace("'", "");
					bufread.close();
					} catch (IOException e) {
						e.printStackTrace();
				}
			}
			
			//check for -o
			if (args[i].toLowerCase().equals("-o")) {
				
				dashO = true;
				if ( i + 1 < (args.length - 1)) {
					i++;
					dashOPath = args[i];
				}
			}	
		}
			
		//finally, we parse the URL itself (the last argument)
		url = args[args.length - 1];
		
		String[] urlSections = url.split("/");
		host = urlSections[2];
		path += "/" + urlSections[urlSections.length-1].replace("'", "");	
	}
	
	public static void transmitMessage(String method) {
		
		//connecting on port 80
		try (Socket socket = new Socket(host, 80);
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));){
		
					out.println(method.toUpperCase() + " " + path + " HTTP/1.0");
					
					if(contentLength)
						out.println("Content-Length: " + data.length());
					out.print(headers);
					
					out.println();
					
					out.println(data);
					String output = "";
					String userInput;
					while ((userInput = in.readLine()) != null) {
						
						if (userInput.contentEquals("")){
							dashV = true;
						}
						if (dashV) {
							output += userInput + "\n";
							System.out.println(userInput);
						}
					}
					
					if (dashO) {
						try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dashOPath), "utf-8"))){
							writer.write(output);
							writer.flush();
						}
					}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
	}
}
