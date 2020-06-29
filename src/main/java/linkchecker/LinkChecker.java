package main.java.linkchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LinkChecker {

    public static void main(String[] args) {

        // Change this path to point to location of docs to be analysed
        final String path = "/Users/stevedare/git/qp-docs/_10.0";
        final String extension = ".md";

        Stats stats = new Stats();
        List<String> fileList = new ArrayList<>();
        ArrayList<Document> documents = processDocuments(path);

        try {
            fileList = Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(filename -> filename.endsWith(extension))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            System.out.println("Cannot get list of files, exiting");
            System.exit(1);
        }

        System.out.println("Processing " + fileList.size() + " files, please wait...");

        for (String file : fileList) {
            Stats fileStats =  processFile(documents, file);
            stats = processStats(stats, fileStats);
        }

        printStats(stats);
    }

    private static Stats processFile(final ArrayList<Document> documents, final String filename) {
        final String categories = "categories: ";
        final String slug = "slug: ";

        String pageCategories = "";
        String pageSlug = "";
        Stats fileStats = new Stats();

        File file = new File(filename);

        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("ERROR - File " + filename + " Not Found");
            return fileStats;
        }

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (line.startsWith(categories)) {
                pageCategories = line.substring(categories.length());
            } else if (line.startsWith(slug)) {
                pageSlug = line.substring(slug.length());
            }

            Pattern pattern = Pattern.compile("\\(.*?\\)");
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String link = matcher.group().substring(1, matcher.group().length() - 1);

                Stats externalLinkStats =  externalLinkValid(filename, link);
                fileStats = processStats(fileStats, externalLinkStats);

                Stats internalLinkStats = internalLinkValid(documents, filename, link, pageCategories, pageSlug);
                fileStats = processStats(fileStats, internalLinkStats);
            }
        }

        return fileStats;
    }

    private static Stats externalLinkValid(final String file, final String link) {
        Stats externalLinkStats = new Stats();

        if (link.startsWith("http") && !link.contains("localhost") && !link.contains("127.0.0.1")) {
            externalLinkStats.numberExternalLinks++;
            try {
                URL url = new URL(link);
                URLConnection conn = url.openConnection();
                conn.connect();
            } catch (java.io.IOException e) {
                System.out.println("EXTERNAL LINK FAIL:  file = " + file + ", link = " + link);
                externalLinkStats.numberExternalLinkFails++;
            }
        }

        return externalLinkStats;
    }

    private static Stats internalLinkValid(final ArrayList<Document> documents, final String file, final String link, final String pageCategories, final String pageSlug) {
        Stats internalLinkStats = new Stats();

        if ((link.startsWith("..") || link.startsWith("#")) &&
                !link.contains(".png") &&
                !link.contains(".svg") &&
                !link.contains(".pdf") &&
                !link.equals("../../api/") &&
                !link.startsWith("../../support") &&
                !link.startsWith("../../tutorials") &&
                !link.equals("../../connectors/") &&
                !link.equals("../../../connectors/") &&
                !link.startsWith("...")) {

            internalLinkStats.numberInternalLinks++;

            Stats internalTrailingStats = checkForInternalLinkTrailingSlash(file, link);
            internalLinkStats.numberInternalLinkTrailingSlash = internalTrailingStats.numberInternalLinkTrailingSlash;

            Stats internalValidLinks = checkInternalLinkValid(documents, file, link, pageCategories, pageSlug);
            internalLinkStats.numberInternalLinkFails = internalValidLinks.numberInternalLinkFails;
        }

        return internalLinkStats;
    }


    private static Stats checkForInternalLinkTrailingSlash(final String file, final String link) {
        Stats internalTrailingLinkStats = new Stats();

        // check the header does not end in a forward slash
        if (link.endsWith("/")) {
            for (int i=link.length()-2; i>=0; i--) {

                if (link.charAt(i) == '#') {
                    System.out.println("TRAILING SLASH FAIL: file = " + file + ", link = " + link);
                    internalTrailingLinkStats.numberInternalLinkTrailingSlash++;
                    break;
                } else if (link.charAt(i) == '/') {
                    break;
                }
            }
        }

        return internalTrailingLinkStats;
    }


    private static Stats checkInternalLinkValid(final ArrayList<Document> documents, final String file, final String link, final String pageCategories, final String pageSlug) {
        Stats internalLinkStats = new Stats();
        String header;
        String[] strArray;
        int srArraySize = 1;

        if (link.contains("/")) {
            strArray = link.split("/");
            srArraySize = strArray.length;
        } else {
            strArray = new String[1];
            strArray[0] = link;
        }

        String linkSlug = pageSlug;
        String linkCategories = pageCategories;
        if (srArraySize > 0 && strArray[srArraySize - 1].startsWith("#")) {
            header = strArray[srArraySize - 1];
            header = header.substring(1);
            srArraySize--;
        } else if (strArray[srArraySize - 1].contains("#")) {
            String[] hashSplit = strArray[srArraySize - 1].split("#");
            header = hashSplit[hashSplit.length - 1];
            strArray[srArraySize - 1] = hashSplit[hashSplit.length - 2];
        } else {
            header = "";
        }

        if (srArraySize > 1) {
            if (!strArray[srArraySize - 1].equals("..")) {
                linkSlug = strArray[srArraySize - 1];
            }

            if (!strArray[srArraySize - 2].equals("..")) {
                linkCategories = strArray[srArraySize - 2];
            }
        } else if (srArraySize == 1) {
            if (!strArray[srArraySize - 1].equals("..")) {
                linkCategories = strArray[srArraySize - 1];
            }
        }

        // cater for additional path in connecting/mq
        if (linkCategories.equals("connecting/mq") && (linkSlug.equals("connectors") || linkSlug.equals("setting-up-connectors"))) {
            linkCategories = "connecting";
        } else if (linkCategories.equals("mq")) {
            linkCategories = "connecting/mq";
        }

        if (!validLink(documents, linkCategories, linkSlug, header)) {
            System.out.println("INTERNAL LINK FAIL: file = " + file + ", link = " + link);
            internalLinkStats.numberInternalLinkFails++;
        }

        return internalLinkStats;
    }


    public static ArrayList<Document> processDocuments(String targetFolder) {
        ArrayList<Document> documents = new ArrayList<>();

        File target = new File(targetFolder);

        scanFolders(target, documents);

        return documents;
    }

    public static boolean validLink(ArrayList<Document> documents, String requiredCategory, String requiredSlug, String requiredTitle) {
        boolean found = false;

        for (Document doc : documents) {
            if (doc.validId(requiredCategory, requiredSlug, requiredTitle)) {
                found = true;
            }
        }
        return found;
    }


    public static String flattenTitle(String title) {
        String flattenTitle = title.replace("#", " ").trim().toLowerCase().replace(" ", "-");
        flattenTitle = flattenTitle.replace("{{site.data.reuse.short_name}}", "event-streams");
        flattenTitle = flattenTitle.replace("{{site.data.reuse.long_name}}", "ibm-event-streams");
        flattenTitle = flattenTitle.replace(":", "");
        flattenTitle = flattenTitle.replace("*", "");
        return flattenTitle;
    }

    public static void scanFolders(File folder, ArrayList<Document> documents) {
        File[] fileSet = folder.listFiles();

        for (int f = 0; f < Objects.requireNonNull(fileSet).length; f++) {
            if (fileSet[f].isFile() && (fileSet[f].getName().endsWith(".md"))) {
                processMDFile(fileSet[f], documents);
            } else if (fileSet[f].isDirectory()) {
                scanFolders(fileSet[f], documents);
            }
        }
    }

    public static void processMDFile(File mdFile, ArrayList<Document> documents) {
        Document document = new Document();
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(mdFile));

            String line = "";

            while (line != null) {
                line = in.readLine();

                if (line != null) {

                    if (line.trim().startsWith("slug:")) {
                        String slug = line.substring(6);
                        document.setSlug(slug);
                    }

                    if (line.trim().startsWith("categories:")) {
                        String category = line.substring(12);
                        document.setCategory(category);
                    }

                    if (line.trim().startsWith("#")) {
                        if (!(line.replace(".", " ").replace("#", " ").trim().isEmpty())) {
                            document.addTitle(flattenTitle(line));
                        }
                    }
                }
            }

            documents.add(document);

        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            if (in != null) {

                try {
                    in.close();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
    }

    private static Stats processStats(final Stats totalStats, final Stats tmpStats) {
        Stats calcStats = new Stats();

        calcStats.numberExternalLinks = totalStats.numberExternalLinks + tmpStats.numberExternalLinks;
        calcStats.numberExternalLinkFails = totalStats.numberExternalLinkFails + tmpStats.numberExternalLinkFails;
        calcStats.numberInternalLinks = totalStats.numberInternalLinks + tmpStats.numberInternalLinks;
        calcStats.numberInternalLinkTrailingSlash = totalStats.numberInternalLinkTrailingSlash + tmpStats.numberInternalLinkTrailingSlash;
        calcStats.numberInternalLinkFails = totalStats.numberInternalLinkFails + tmpStats.numberInternalLinkFails;

        return calcStats;
    }

    private static void printStats(final Stats stats) {

        System.out.println("-----------------------");
        System.out.println("Number of links checked: " + (stats.numberExternalLinks + stats.numberInternalLinks));
        System.out.println("Number of external links checked: " + stats.numberExternalLinks);
        System.out.println("Number of internal links checked: " + stats.numberInternalLinks);

        System.out.println("Number of external failed links: " + stats.numberExternalLinkFails);
        System.out.println("Number of internal link with trailing slash: " + stats.numberInternalLinkTrailingSlash);
        System.out.println("Number of internal failed links: " + stats.numberInternalLinkFails);
        System.out.println("-----------------------");

        if (stats.numberExternalLinkFails == 0 &&
                stats.numberInternalLinkTrailingSlash == 0 &&
                stats.numberInternalLinkFails == 0) {
            System.out.println("All links are ok.");
        } else {
            System.out.println("There are link problems");
        }
    }
}

class Stats {
    int numberExternalLinks;
    int numberExternalLinkFails;

    int numberInternalLinks;
    int numberInternalLinkFails;
    int numberInternalLinkTrailingSlash;
}
