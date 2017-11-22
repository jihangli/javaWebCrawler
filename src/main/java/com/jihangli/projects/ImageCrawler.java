package com.jihangli.projects;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.List;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * A toy program to learn WebMagic, getting familiar with java regex and xpath
 *
 */
public class ImageCrawler implements PageProcessor {

    //@TODO: Make these variables configurable through command or UI
    private Site site = Site.me().setRetryTimes(1).setSleepTime(5000).setTimeOut(5000);
    private static final String WEBPAGE_TO_CRAWL = "http://example.org/";
    private static final String LOCATION_PREFIX = "/Users/example";
    private static final String URL_REGEX = "http://example\\.org/page/";
    private static String DATE_TIME;

    /**
     * Constructor
     */
    public ImageCrawler() {
        DATE_TIME = LocalDateTime.now().toString();
    }

    /**
     * Process images from tumblr
     * @param page
     */
    @Override
    public void process(Page page) {
        StringBuilder nextPageUrl = new StringBuilder(URL_REGEX);

        try {
            nextPageUrl.append(getCurrentPageNumber(page) + 1);
            System.out.println("adding a new page to task queue: " + nextPageUrl);
        } catch (Exception e) {
            System.out.println("error" + e.getMessage());
            return;
        }
        List<String> newRequest = page.getHtml()
                .links()
                .regex(nextPageUrl.toString())
                .all();

       page.addTargetRequests(newRequest);

        List<String> urlList =
                page.getHtml().xpath("//img/@src").all();
        try {
            downLoadPics(urlList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return current site
     */
    @Override
    public Site getSite() {
        return site;
    }

    /**
     * Parse page and get the current page number
     * @param page - current page being processed
     * @return current page number
     */
    public int getCurrentPageNumber(Page page) {
        String url = page.getUrl().toString();
        Pattern p = Pattern.compile("page/\\d+");
        Matcher m = p.matcher(url);

        if(m.find()) {
            if(m.groupCount() > 1) {
                throw new RuntimeException("Error, found multiple page number in the given url.");
            }

            String pageInfo = m.group(0);
            return Integer.valueOf(StringUtils.right(pageInfo, pageInfo.length() - "page/".length()));
        }
        //first page does not have a page number, just return 1
        return 1;
    }

    /**
     * Downloading photos and saved to pre-defined location
     * @param imgUrls - input image urls
     * @throws Exception
     */
    public void downLoadPics(List<String> imgUrls) throws Exception {
        String dir = LOCATION_PREFIX + "_" + DATE_TIME;

        for (int counter = 1; counter < imgUrls.size(); counter++) {
            URL url = new URL(imgUrls.get(counter));
            DataInputStream dis = new DataInputStream(url.openStream());

            int randomNumber = (int)(Math.random() * 1000000);
            String newImageName = dir + "/" + randomNumber + "_" + counter + ".jpg";

            FileOutputStream fos = new FileOutputStream(new File(newImageName));
            byte[] buffer = new byte[1024];
            int length;

            while ((length = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            dis.close();
            fos.close();
        }
    }

    public static void main(String[] args) {
        ImageCrawler crawler = new ImageCrawler();
        File fileDir = new File(LOCATION_PREFIX + "_" + DATE_TIME);
        fileDir.mkdirs();

        Spider.create(crawler)
                .addUrl(WEBPAGE_TO_CRAWL)
                .thread(10)
                .run();

    }

}
