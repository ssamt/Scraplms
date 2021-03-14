import java.io.IOException;

public class Scraplms {
    public static void main(String[] args) throws IOException {
        DownloadFrame f = new DownloadFrame();
        f.f.setVisible(true);
        HttpDownloadUtility downloadUtility = new HttpDownloadUtility();
        HttpDownloadUtility.downloadFile("http://lms.ksa.hs.kr/NBoard/download.php?db=vod&idx=88184&fnum=2",
                "C:\\My Folder\\KSA");
    }
}
