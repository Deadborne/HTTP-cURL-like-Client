package httpc;

import java.io.*;
import java.net.*;


public class httpc {

	public static void main(String[] args) {
		
		//The command will be the first argument. The Request will be the second.
		BufferedReader bufread;
		
		//identifying whether this is a get or post request
		String httpMethod = args[0].toLowerCase(); 
		boolean isGetPost = httpMethod.equals("get") || httpMethod.equals("post");
		
		//initializing all the components we'll need later
		String path = "", url = "", host = "", headers = "", data = "", dashOPath = ""; 
		boolean contentLength = true;
		boolean dashV = false, dashO = false;
		
		//After the second argument, everything else is parsed within the following block.
		if (isGetPost) {
			
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
			
			System.out.println("Start:");
			
			//connecting on port 80
			try (Socket socket = new Socket(host, 80);
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));){
			
						out.println(httpMethod.toUpperCase() + " " + path + " HTTP/1.0");
						
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
							}
						}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		//other than GET or POST, we can respond to a HELP request
		else if(httpMethod.equals("help")) 							
			httpcMethods.helpMe(args);			
	}
}
