package playground;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;

//-------------------------------------------------------
// Auto-threaded application to download all the SOHO sunspot image
// into a folder and auto generates an animated image
// NOTE: Uses maximum threads available 
///-------------------------------------------------------
public class SohoSunspotAnimator {

	
	// Set all the following variables
	static String storage_directory = "<DOWNLOAD DIRECTORY>";
	static String starting_date = "202410"; // replace with prefix YYYY, MM, DD - 
	static String size = "1024"; // replace with either 1024 or 512
	static int timeout = 5; // number of minutes
	static int sleep_time = 500; // milliseconds of sleep time
	static int gif_delay = 100; // milliseconds of delay
	// --------------------------
	
	static String baseURL = "https://soho.nascom.nasa.gov/data/synoptic/sunspots_earth/";
	static Lock newLock = new ReentrantLock();
	static HashSet<String> deadLetter = new HashSet<String>();
	static HashSet<String> filesExist = new HashSet<String>();
	static ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue();
	static String fileFolder = "";
	static int filesSkipped = 0;
	static int countDownloaded = 0;
	
	public static void main(String[] args) {
		
		System.out.println("Beginning Work: " + filesExist.size());
		try {
			filesExist = getFiles(storage_directory);
			retrieveDirectory();
			System.out.println("Current Files: " + filesExist.size());
			System.out.println("Found Files: " + (queue.size() + filesSkipped));
			System.out.println("Skipping Files: " + filesSkipped);
			downloadFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}
	
	private static void makeImage(ArrayList<String> ordered_files) {
		
		BufferedImage first = null;
		try {
			first = ImageIO.read(new File(storage_directory + ordered_files.get(0)));
		} catch (IOException e) {
			e.printStackTrace();
		}
        ImageOutputStream output = null;
		try {
			output = new FileImageOutputStream(new File(storage_directory + "animated-1.gif"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

        GifSequenceWriter writer = null;
		try {
			writer = new GifSequenceWriter(output, first.getType(), gif_delay, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
        try {
			writer.writeToSequence(first);
		} catch (IOException e) {
			e.printStackTrace();
		}
        ordered_files.remove(0);
       
        for (String image : ordered_files) {
            BufferedImage next = null;
			try {
				next = ImageIO.read(new File(storage_directory + image));
			} catch (IOException e) {
				e.printStackTrace();
			}
            try {
				writer.writeToSequence(next);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }

        try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        try {
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static ArrayList<String> processFiles() {
		ArrayList<String> all_files = new ArrayList<String>(filesExist);
		ArrayList<String> temp_files = new ArrayList<String>();
		for (String filename : all_files) {
			if (filename.contains(".jpg") && filename.contains("sunspots_"+size+"_")) {
				temp_files.add(filename);
			}
		}
		
		Collections.sort(temp_files);
		return temp_files;
	}

	private static void downloadFiles() {
		
		int threads = Runtime.getRuntime().availableProcessors();
		System.out.println("Total Threads: " + threads);
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		for (int i = 0; i < threads ; i++) {
		executor.execute(()->{
			    downloadFile();
			    try {
					Thread.sleep(gif_delay);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  });		
		}
		executor.shutdown();
		try {
			executor.awaitTermination(timeout, TimeUnit.MINUTES );
			
			
			try {
				filesExist = getFiles(storage_directory);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(5000);
				ArrayList<String> ordered_files = processFiles();
				System.out.println("Total Downloaded: " + countDownloaded);
				System.out.println("Potential Errors: " + deadLetter);
				makeImage(ordered_files);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
			
	}

	private static void downloadFile(){
		while(!queue.isEmpty()) {
		String file = queue.poll();
		if (newLock.tryLock()) {
			   try {
				   if (!deadLetter.contains(file))
				   deadLetter.add(file);
			   } finally {
				   newLock.unlock();
				   if (downloadFile(file)) {
					   if (newLock.tryLock()) {
						   try {
							   countDownloaded++;
							   deadLetter.remove(file);
						   } finally {
							   newLock.unlock();
						   }
					   }
				   } else {
						 queue.add(file);
				   }
			   }
			 } 
		try {
			Thread.sleep(sleep_time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		};
		}
		return;
	}

	private static boolean downloadFile(String file) {
		URL website;
		try {
			website = new URL(baseURL + file);
		} catch (MalformedURLException e) {
			return false;
		}
		ReadableByteChannel rbc;
		try {
			rbc = Channels.newChannel(website.openStream());
		} catch (IOException e) {
			return false;
		}
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(storage_directory + file);
		} catch (FileNotFoundException e) {
			return false;
		}
		try {
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static HashSet<String> getFiles(String directory) throws IOException {

		Path path = Paths.get(directory);
        ArrayList<String> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = (ArrayList<String>) walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList()).stream().map(p -> p.getFileName()).map(p -> p.toString())
                    .collect(Collectors.toList());
        }
        return new HashSet<String>(result);

    }

	public static void retrieveDirectory() {
		
		URL url = null;
		try {
			url = new URL("https://soho.nascom.nasa.gov/data/synoptic/sunspots_earth/");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		boolean start_capture = false;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
		    for (String line; (line = reader.readLine()) != null;) {
		        if (line.contains(">sunspots_" + size)) {
		        	line = line.substring(line.indexOf(">sunspots_"+size+"_")+1,line.indexOf(".jpg<")+4);
		        	if (line.indexOf(starting_date) > 0) {
		        		start_capture = true;
		        	}
		        	if (start_capture && !filesExist.contains(line))
		        		queue.add(line);
		        	else 
		        		filesSkipped++;
		        }
		    }
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
