// Quick and dirty code to fetch all books from http://kutub.info

import java.io.*;
import java.util.*;
import java.net.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Demo {	
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final String OUTPUT_FOLDER = "OLL\\";
	
	public static void main(String... args) throws Exception {	
		
 		String url = "";
		new File(OUTPUT_FOLDER).mkdir();
		
		for(int i=1; i<16500; i++) {
			try {
				url = "http://www.kutub.info/library/book/" + i;
				downloadBook(url);
			}
			catch(Exception e) {				
				System.out.println("Not Found: " + url);
			}			
		}		
	}	

	private static void reDownloadFiles(String filename) throws Exception {
		int countOfDownloads = 0;
		List<String> filesNotDownloaded = new ArrayList<String>();
		
		Scanner input = new Scanner(new File(filename));
		while(input.hasNext()) {
			String line = input.nextLine();
			if ( !line.trim().isEmpty() ) {
				if ( line.startsWith("Download") ) {
					countOfDownloads++;
				}
				else if ( line.startsWith("Not Found") ) {
					filesNotDownloaded.add(line);
				}
			}
		}
		
		System.out.println("Number of files downloaded: " + countOfDownloads);
		System.out.println("Number of files not downloaded: " + filesNotDownloaded.size());
		
		for(String file: filesNotDownloaded) {
			String name = file.split("Not Found:")[1].trim();
			try {
				downloadBook(name);				
			}
			catch(Exception e) {
				e.printStackTrace();
				System.out.println("Not Found: " + name);
			}	
		}
	}
	
	public static void downloadBook(String url) throws Exception {
		Book book = GetBook(url);
				
		if( book.downloadLink != null) {			
			createFolders(book);
			sendGet(book.downloadLink, book);
			System.out.println("Download: " + book.downloadLink);
			System.out.println();
		}
	}
	
	private static class Book {
		String downloadLink;
		String title;
		String parentFolder;
		String childFolder;
	}
	
	private static void createFolders(Book book) {
		File dir = new File(OUTPUT_FOLDER + book.parentFolder);
		if (!dir.exists() )
			dir.mkdir();
			
		if ( book.childFolder != null ) {
			File childDir = new File(OUTPUT_FOLDER + book.parentFolder + "\\" + book.childFolder);
			if (!childDir.exists() )
				childDir.mkdir();
		}
	}
	
	private static String normalizeFileName(String orignalName) {
		StringBuilder filename = new StringBuilder();

		for (char c : orignalName.toCharArray()) {
		  if (c =='.' ||  c== ' ' || Character.isJavaIdentifierPart(c)) {
			filename.append(c);
		  }
		}
		
		return filename.toString();
	}
	
	private static Book GetBook(String url) throws IOException{
		String downloadLink = null;
		Book book = new Book();
		
        Document doc = Jsoup.connect(url).get();
		
		String title = doc.title();
        Elements links = doc.select("a[href]");

		boolean foundParent = false;
        for (Element link : links) {            
			if(link.attr("abs:href").startsWith("http://www.kutub.info/downloads")) {
				downloadLink = link.attr("abs:href");				
			}
			else if ((link.attr("abs:href").contains("library/category"))) {
				if (foundParent)
					book.childFolder = link.text();
				else {
					book.parentFolder = link.text();
					foundParent = true;
				}
			}
        }
		
		book.title = title;
		book.downloadLink = downloadLink;
		
		return book;
	}
	
	private static String getBookPath(Book book, String extension) {
		String name = normalizeFileName(book.title);
		
		if ( book.childFolder == null) {
			return OUTPUT_FOLDER + book.parentFolder + "\\" + name + extension;
		}
		
		return OUTPUT_FOLDER + book.parentFolder + "\\" + book.childFolder +  "\\" + name + extension;		
	}
	
	private static void sendGet(String url, Book book ) throws Exception {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
 
		int responseCode = con.getResponseCode();
		//System.out.println("Response Code : " + responseCode);
		
		int part = url.lastIndexOf(".");
		int part2 = url.indexOf("?");
		String ext = url.substring(part, part2);
		
		String filename = getBookPath(book, ext);
		
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			is = con.getInputStream();            
			fos = new FileOutputStream(filename);

			byte[] buffer = new byte[4096];           
			int len;

			while ((len = is.read(buffer)) > 0) {  
				fos.write(buffer, 0, len);
			}
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
		}
	}
}