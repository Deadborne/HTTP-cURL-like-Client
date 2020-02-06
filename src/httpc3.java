import java.io.*;
import java.net.*;

public class httpc3 {

	private static Socket socket = new Socket();
	
	private static boolean dashV = false, helpMe = false, dashH = false, dashD = false, dashF = false, isGet = false, isPost = false;
	private static String headerInput = "", url = "", inline = "", filePath = "", hostName = "", directory = "", arguments = "", message = "";
	private static String portNumber = "80";
	private static String[] hostArgs = new String[2];

	//Used for parsing the URL to which the request is made
	public static void parseURL (String url) {
		
		if (url.contains("../")) {
			hostArgs = url.split("/", 2); //split the url into the directory and its arguments
			directory = hostArgs[0];
			arguments = hostArgs[1];
		}
		
		else if (url.contains("//")) {
			
			hostArgs = url.split("//");
			
			if (url.contains("/")) {
				hostArgs = hostArgs[1].split("/");
				hostName = hostArgs[0];
				arguments = hostArgs[1];
			}
		}
		
		else if (url.contains("/")) {
			hostArgs = url.split("/", 2);
			hostName = hostArgs[0];
			arguments = hostArgs[1];
		}
		
		//otherwise, we can take the url as it is
		else {
			hostName = url;
		}
		
		if (hostName.contains("localHost")) {
			hostArgs = hostName.split(":", 2);
			hostName = hostArgs[0];
			portNumber = hostArgs[1];
		}
		
	}
	
	//identify whether it's GET, POST, or help, on top of -v, -h, -d, -f.
	public static void parseMethod(String[] args) {
		
		for (int i = 0; i < args.length; i++) {
			
			args[i] = args[i].toLowerCase();
			
			if (args[i].equals("-v"))
				dashV = true;
			else if (args[i].equals("-h")) {
				dashH = true;
				headerInput = headerInput.concat(args[i+1] + "\r\n");
				i++;
			}
			else if (args[i].equals("-d")) {
				dashD = true;
				inline = (args[i+1]);
				i++;
			}
			else if (args[i].equals("-f")) {
				dashF = true;
				filePath = (args[i+1]);
				i++;
			}
			else if (args[i].equals("get"))
				isGet = true;
			else if (args[i].equals("post"))
				isPost = true;
			else if (args[i].equals("help"))
				helpMe = true;
			else
				url = args[i];		
		}
	}
	
	//method for processing -d
	public static String dashDMethod(String inline){
		
		inline = inline.replaceAll("\\s", ""); //cut the whitespaces
		String parameter = "";
		
		if (inline.charAt(0) == '{') {
			inline = inline.substring (1, inline.length()-1);
		}
		
		String[] stringArraySections = inline.split("&|,|\n");
		
		try {
			for (String s: stringArraySections) {
				
				String[] smallerSections = s.split("=|:");
				for (String s1: smallerSections) {
					if (s1.charAt(0) == '"') {
						s1 = s1.substring(1, s1.length()-1);
					}
					parameter = parameter.concat(URLEncoder.encode(s1, "UTF-8"));
					parameter += "=";
				}
				
				parameter = parameter.substring(0, parameter.length() - 1);
				parameter += "&";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return parameter.substring(0, parameter.length() - 1);
	}
	
	//method for processing -f
	public static String dashFMethod(String filePath) {
		
		String returnLine = "";
		
		try {
			File file = new File(filePath);
			BufferedReader bufRead = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String inputLine;
			
			while ((inputLine = bufRead.readLine()) != null) {
				returnLine += inputLine;
			}
			
			bufRead.close();
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return returnLine;
	}
	
	//method for building the total message that we can then send through the socket
	public static String messageBuilder (boolean header, boolean data, String requestMethod, String arguments) {
		
		String builtMessage = "";
		
		if (requestMethod == "GET /") {
			builtMessage = "GET " + directory + "/" + arguments + " HTTP/1.0\r\n\r\n";
			if (header) {
				builtMessage = builtMessage.replace("\r\n\r\n", ("\r\n" + headerInput + "\r\n"));
			}		
		} else {
			builtMessage = requestMethod + arguments + " HTTP/1.0\r\n";
			builtMessage += "Content-Length: " + inline.length() + "\r\n" ;
			builtMessage += headerInput + "\r\n";
		
			if (data) {
				builtMessage += inline + "\r\n";
			}
		}
		
		builtMessage += "\r\n";
		System.out.println(builtMessage);
		return builtMessage;	
	}
	
	//After having built the message, we can send it through the socket. Go!
	public static void messageSender(String messageToSend) {
		
		try {
			socket.connect(new InetSocketAddress(hostName, Integer.parseInt(portNumber)));

			String response = " ";
			
			BufferedReader bufRead = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter bufWrite = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			bufWrite.write(messageToSend);
			bufWrite.flush();

			
			while ((response = bufRead.readLine()) != null) {
				
				if ((response.length() == 0) && !dashV){
					StringBuilder received = new StringBuilder();
					
					while ((response = bufRead.readLine()) != null) {
						received.append(response);
						received.append("\r\n");
					}
					System.out.println(received.toString());
					dashV = false;
					break;
				}
					
				else if (dashV) 
					System.out.println(response);
			}
					
				bufWrite.close();
				bufRead.close();
				socket.close();
					
			} catch (Exception e) {
				e.printStackTrace();
		}
	}
	
	//This is the bulk of the GET method
	public static void get(String inputURL) {
		
		parseURL(inputURL);
		message = messageBuilder(dashH, false, "GET / ", arguments);
		messageSender(message);
		
	}
	
	//This is the bulk of the POST method
	public static void post(String inputURL) {
		
		parseURL(inputURL);
		
		if (dashD && dashF) {
			System.out.println("-d and -f are incompatible together. Goodbye!");
			System.exit(1);
		}
		else if (dashF) {
			dashD = true;
			inline = dashFMethod(filePath);
			inline = dashDMethod(inline);
		}
		else if (dashD)
			inline = dashDMethod(inline);
		
		message = messageBuilder(dashH, dashD, "POST /", arguments);
		messageSender(message);

	}
	
	
	//help menu method
	public static void helpMenu() {
		
        String help = "\nhttpc help\n" 
                +"\nhttpc is a curl-like application but supports HTTP protocol only.\n"
                +"Usage:\n"
                +"\t httpc command [arguments]\n"
                +"The commands are:\n"
                +"\t get \t executes a HTTP GET request and prints the response.\n"
                +"\t post \t executes a HTTP POST request and prints the response.\n"
                +"\t help \t prints this screen.\n"
                +"\nUse \"httpc help [command]\" for more information about a command.\n";

        String getHelp = "\nhttpc help get\n"
                +"\nusage: httpc get [-v] [-h key:value] URL\n"
                +"\nGet executes a HTTP GET request for a given URL.\n"
                +"\n-v Prints the detail of the response such as protocol, status, and headers.\n"
                +"-h key:value Associates headers to HTTP Request with the format 'key:value'.\n";

        String postHelp = "\nhttpc help post\n"
                +"\nusage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n"
                +"\nPost executes a HTTP POST request for a given URL with inline data or from file.\n"
                +"\n-v Prints the detail of the response such as protocol, status, and headers.\n"
                +"-h key:value Associates headers to HTTP Request with the format 'key:value'.\n"
                +"-d string Associates an inline data to the body HTTP POST request.\n"
                +"-f file Associates the content of a file to the body HTTP POST request.\n"
                +"\nEither [-d] or [-f] can be used but not both.\n";
        
        if (isGet) {
        	System.out.println(getHelp);
        	System.exit(0);
        }
        else if (isPost) {
        	System.out.println(postHelp);
        	System.exit(0);	
        }
        else 
        	System.out.println(help);
        	System.exit(0);	
	}
	
	
	public static void main(String[] args) {
		
		//First we break down the arguments. If none are provided, help menu is opened.
		if (args.length > 0) 
			parseMethod(args);
		else 
			helpMenu();
		
		//next we launch the method based on what we parsed.
		if (isGet)
			get(url);
		else if (isPost)
			post(url);
		else if (helpMe)
			helpMenu();
	}
	
}
