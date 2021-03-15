import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class Scraplms {
    public static void main(String[] args) throws IOException {
        DownloadFrame f = new DownloadFrame();
        f.id = "20-017";
        f.pw = "YYd3RKVK7KpMb3u";
        f.session = f.loginSession();
        f.scBCate = "1565";
        f.from = 3;
        f.to = 6;
        System.out.print(f.allFileUrls());
    }
}
