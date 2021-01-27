package youwu;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import utils.HttpPools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class YouWuCrawler {

//    private static String base_uel = "https://www.youwu333.com/x/91/";
    private static String base_path = "D:\\img\\youwu";

    public static void main(String[] args) {
        String url = "https://www.youwu333.com/x/98/";
        builder(url);
    }

    /**
     * 获取所有机构信息，导航栏
     */
    private static void getJiGou(String url,List<Map<String,String>> datas) {
//        url = "https://www.youwu333.com/x/98/";
        String httpGet = HttpPools.httpGet(url);
        Document document = Jsoup.parse(httpGet);
        Elements elements = document.select(".jigou").select("li");
        List<Map<String, String>> collect = elements.parallelStream().map(element -> {
            String href = element.select("a").attr("href");
            String title = element.select("a").text();
            String size = element.select("span").text().split(" ")[0].trim();
            Map<String, String> jigou = new HashMap<>();
            jigou.put("href", href);
            jigou.put("title", title);
            jigou.put("size", size);
            return jigou;
        }).collect(Collectors.toList());
        collect.remove(0);

        datas.addAll(collect);
    }

    /**
     * 具体导航栏下的美女链接，获取美女页面
     *
     * @param url
     */
    public static void getGirle(String url,String baseUrl, List<Map<String,String>> datas) {
        String httpGet = HttpPools.httpGet(url);
        Document document = Jsoup.parse(httpGet);

        Elements elements = document.select(".pic ul li");
        List<Map<String, String>> collect = elements.parallelStream().map(element -> {
            // 图片地址
            String href = element.select("a").attr("href");
            String img_src = element.select("a img").attr("src");
            String title = element.select(".title a").text();
            String num = element.select(".view").text();
            Map<String, String> img = new HashMap<>();
            img.put("href", href);
            img.put("img_src", img_src);
            img.put("title", title);
            img.put("num", num);
            return img;
        }).collect(Collectors.toList());
        datas.addAll(collect);

        Elements select = document.select(".pages-show > a");
        String href = select.last().attr("href");
        if (!StringUtil.isBlank(href)) {
            //获取下一页
            Elements select1 = select.select(":containsOwn(下一页)");
            String next_href = baseUrl + select1.attr("href");
            System.out.println(next_href);
            //递归 翻页
            getGirle(next_href,baseUrl,datas);
        }

        return;
    }

    public static void downloadImg(String url,String jg,String title,String baseUrl) {
        String httpGet = HttpPools.httpGet(url);
        if (StringUtil.isBlank(httpGet)){
            System.out.println("请求地址为空："+url);
        }else{

            Document document = Jsoup.parse(httpGet);
            Elements elements = document.select(".content img");
            elements.parallelStream().peek(element -> {
                String src = element.attr("src");
                String[] split = src.split("/");
                String fileName = split[split.length - 1];
                String referer = baseUrl + "?img=" + src;
                StringBuilder dir = new StringBuilder(base_path);
                dir.append(File.separator)
                        .append(jg)
                        .append(File.separator)
                        .append(title)
                        .append(File.separator);
                File file = new File(dir.toString());
                if (!file.exists()){
                    file.mkdirs();
                }
                dir.append(fileName);
                File fileDir = new File(dir.toString());
                if (!fileDir.exists() || fileDir.length() == 0){
                    try (FileOutputStream fileOutputStream = new FileOutputStream(fileDir)) {
                        byte[] bytes = HttpPools.httpGetImg(src, referer);
                        if (bytes != null){
                            ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
                            buffer.put(bytes);
                            buffer.flip();  //准备 读取
                            FileChannel channel = fileOutputStream.getChannel();
                            channel.write(buffer);
                        }else{
                            System.out.println("获取图片为空："+src);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    System.out.println("文件已存在");
                }
            }).count();

            // 获取翻页信息
            Elements selects = document.select(".pages-show a");

            Elements select = selects.select(":containsOwn(下一页)");
            System.out.println(select);
            System.out.println(!Objects.isNull(select) && !StringUtil.isBlank(select.attr("href")));
            if (!Objects.isNull(select) && !StringUtil.isBlank(select.attr("href"))){
                System.out.println("下一页："+select.last().attr("href"));
                String next_href = baseUrl + select.last().attr("href");
                System.out.println(next_href);
                downloadImg(next_href,jg,title,baseUrl);
            }else{
                return;
            }
        }
    }

    public static void builder(String url){
        //首先获取机构信息

        List<Map<String,String>> datas = new ArrayList<>();
        getJiGou(url,datas);

        //获取美女链接
        datas.stream().peek(jigou -> {
            String jgHref = jigou.get("href");
            String JGtitle = jigou.get("title");
            List<Map<String,String>> girleData = new ArrayList<>();
            getGirle(jgHref,jgHref,girleData);
            girleData.parallelStream().peek(girle -> {
                String girleHref = girle.get("href");
                String girleTile = girle.get("title");
                //下载美女图片
                downloadImg(girleHref, JGtitle, girleTile,jgHref);
            }).count();
        }).count();


    }
}
