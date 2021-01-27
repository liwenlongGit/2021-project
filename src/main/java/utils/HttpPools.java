package utils;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * http连接池
 *
 * @author Administrator
 */
@Slf4j
public class HttpPools {
    /**
     * 池化管理
     */
    private static PoolingHttpClientConnectionManager poolConnManager = null;

    /**
     * 它是线程安全的，所有的线程都可以使用它一起发送http请求
     */
    private static CloseableHttpClient httpClient;

    static {
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
            // 配置同时支持 HTTP 和 HTPPS
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslsf)
                    .build();
            // 初始化连接管理器
            poolConnManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            //  设置socket配置
            SocketConfig socketConfig = SocketConfig.custom()
                    .setTcpNoDelay(true)
                    .build();
            poolConnManager.setDefaultSocketConfig(socketConfig);
            // 同时最多连接数
            poolConnManager.setMaxTotal(640);
            // 设置最大路由
            poolConnManager.setDefaultMaxPerRoute(320);
            httpClient = getConnection();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public static CloseableHttpClient getConnection() {
        RequestConfig config = RequestConfig
                .custom()
                .setConnectTimeout(50000)
                .setConnectionRequestTimeout(50000)
                .setSocketTimeout(50000)
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                // 设置连接池管理
                .setConnectionManager(poolConnManager)
                .setDefaultRequestConfig(config)
                // 设置重试次数
                .setRetryHandler(new DefaultHttpRequestRetryHandler(10, false)).build();
        return httpClient;
    }

    public static String httpGet(String url) {

        HttpGet httpGet = new HttpGet(url);
        //設置httpGet的头部參數信息
        httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        httpGet.setHeader("Accept-Charset", "GB2312,utf-8;q=0.7,*;q=0.7");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
        httpGet.setHeader("Accept-Language", "zh-cn,zh;q=0.5");
        httpGet.setHeader("Connection", "keep-alive");
        httpGet.setHeader("Cookie", "UM_distinctid=177243a9dab1ff-05b44a19aac5c9-376b4502-7e9000-177243a9dac744; CNZZDATA1276462515=272877523-1611219409-https%253A%252F%252Fwww.so.com%252F%7C1611219409");
        httpGet.setHeader(":authority", "www.youwu333.com");
        httpGet.setHeader("referer", "https://www.youwu333.com/x/5/list_5_1.html");
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            String result = EntityUtils.toString(response.getEntity(),"gb2312");
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpStatus.SC_OK) {
                return result;
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error("发送请求失败{}", url);
        }
        return null;
    }
    public static byte[] httpGetImg(String url, String referer) {
        HttpGet httpGet = new HttpGet(url);
        //設置httpGet的头部參數信息
        httpGet.setHeader("referer", referer);
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpStatus.SC_OK || code == HttpStatus.SC_NOT_MODIFIED) {
                return EntityUtils.toByteArray(response.getEntity());
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error("发送请求失败{}", url);
        }
        return null;
    }

    @SneakyThrows
    public static String post(String uri, Object params, Header... heads) {
        HttpPost httpPost = new HttpPost(uri);
        StringEntity paramEntity = new StringEntity(JSON.toJSONString(params));
        paramEntity.setContentEncoding("UTF-8");
        paramEntity.setContentType("application/json");
        httpPost.setEntity(paramEntity);
        if (heads != null) {
            httpPost.setHeaders(heads);
        }
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {


            int code = response.getStatusLine().getStatusCode();
            String result = EntityUtils.toString(response.getEntity());
            if (code == HttpStatus.SC_OK) {
                return result;
            } else {
                log.info("获取数据状态码不正确{}", code);
                return null;
            }
        } catch (IOException e) {
            log.error("发送请求失败{}", uri);
        }
        return null;
    }
}
