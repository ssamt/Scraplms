import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class DownloadFrame {
    JFrame f;
    JTextField urlField;
    JTextField folderField;
    JTextField fromField;
    JTextField toField;
    JTextField idField;
    JPasswordField pwField;
    JLabel messageLabel;

    static FlowLayout leftLayout = new FlowLayout(FlowLayout.LEFT);

    String scBCate;
    String folderPath;
    int from, to;
    String id, pw;

    Connection.Response session;

    static String mainUrl = "http://lms.ksa.hs.kr";
    static String loginUrl = mainUrl + "/Source/Include/login_ok.php";
    static String boardUrl = mainUrl + "/nboard.php";

    public DownloadFrame() {
        f = new JFrame();
        f.setSize(800, 400);
        f.setLayout(new GridLayout(10, 1, 0, 3));
        f.add(createUrlPanel());
        f.add(createFolderPanel());
        f.add(createIndexPanel());
        f.add(createIdPwPanel());
        f.add(createDownloadPanel());
    }

    private JPanel createUrlPanel() {
        JPanel urlPanel = new JPanel(leftLayout);
        JLabel urlLabel = new JLabel("URL: ");
        urlPanel.add(urlLabel);
        urlField = new JTextField("", 40);
        urlField.setBounds(100, 100, 400, 40);
        urlPanel.add(urlField);
        return urlPanel;
    }

    private JPanel createFolderPanel() {
        JPanel folderPanel = new JPanel(leftLayout);
        JLabel folderLabel = new JLabel("폴더: ");
        folderPanel.add(folderLabel);
        folderField = new JTextField("", 40);
        folderPanel.add(folderField);
        JButton folderButton = new JButton("...");
        folderButton.addActionListener(actionEvent -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (folderChooser.showOpenDialog(folderPanel) == JFileChooser.APPROVE_OPTION) {
                folderField.setText(String.valueOf(folderChooser.getSelectedFile()));
            }
        });
        folderPanel.add(folderButton);
        return folderPanel;
    }

    private JPanel createIndexPanel() {
        JPanel indexPanel = new JPanel(leftLayout);
        JLabel indexLabel1 = new JLabel("번호: ");
        indexPanel.add(indexLabel1);
        fromField = new JTextField("", 5);
        indexPanel.add(fromField);
        JLabel indexLabel2 = new JLabel("~");
        indexPanel.add(indexLabel2);
        toField = new JTextField("", 5);
        indexPanel.add(toField);
        return indexPanel;
    }

    private JPanel createIdPwPanel() {
        JPanel IdPwPanel = new JPanel(leftLayout);
        JLabel idLabel = new JLabel("아이디: ");
        IdPwPanel.add(idLabel);
        idField = new JTextField("", 10);
        IdPwPanel.add(idField);
        JLabel pwLabel = new JLabel("비밀번호: ");
        IdPwPanel.add(pwLabel);
        pwField = new JPasswordField("", 10);
        IdPwPanel.add(pwField);
        return IdPwPanel;
    }

    private JPanel createDownloadPanel() {
        JPanel downloadPanel = new JPanel(leftLayout);
        JButton downloadButton = new JButton("다운로드");
        downloadButton.addActionListener(actionEvent -> {
            scBCate = getScBCate(urlField.toString());
            folderPath = folderField.toString();
            if (fromField.toString().equals("")) {
                from = 0;
            } else {
                from = Integer.parseInt(fromField.toString());
            }
            if (toField.toString().equals("")) {
                to = 0;
            } else {
                to = Integer.parseInt(toField.toString());
            }
            id = idField.toString();
            pw = pwField.toString();
            try {
                download();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        downloadPanel.add(downloadButton);
        messageLabel = new JLabel("");
        downloadPanel.add(messageLabel);
        return downloadPanel;
    }

    public String getScBCate(String urlString) {
        try {
            String toFind = "scBCate";
            URL url = new URL(urlString);
            String query = url.getQuery();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                if (pair.startsWith(toFind)) {
                    return pair.substring(toFind.length()+1);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void download() throws IOException {
        messageLabel.setText("로그인 중...");
        session = loginSession();
        messageLabel.setText("파일 위치 탐색 중...");
        ArrayList<String> fileUrls = allFileUrls();
        for (int i=0; i<fileUrls.size(); i++) {
            messageLabel.setText(String.format("다운로드 중... %d/%d", i+1, fileUrls.size()));
            HttpDownloadUtility.downloadFile(fileUrls.get(i), folderPath);
        }
        messageLabel.setText("");
        JOptionPane.showMessageDialog(null, "다운로드 완료");
    }

    public Connection.Response loginSession() throws IOException {
        return Jsoup.connect(loginUrl)
                .data("user_id", this.id)
                .data("user_pwd", this.pw)
                .method(Connection.Method.POST)
                .execute();
    }

    public ArrayList<String> allFileUrls() throws IOException {
        Document firstPage = Jsoup.connect(boardUrl).data("db", "vod").data("scBCate", scBCate)
                .cookies(session.cookies()).get();
        int pagesNum = boardPagesNum(firstPage);
        int postsNum = boardPostsNum(firstPage);
        int postsInPage = 20;
        if (from == 0) {
            from = 1;
        }
        if (to == 0) {
            to = postsNum;
        }
        ArrayList<String> fileUrls = new ArrayList<>();
        int startPage = (postsNum-to)/postsInPage+1;
        int endPage = (postsNum-from)/postsInPage+1;
        Document pageDoc;
        for (int page=startPage, postIndex=postsNum-(startPage-1)*postsInPage; page<=endPage; page++) {
            pageDoc = Jsoup.connect(boardUrl).data("page", Integer.toString(page)).data("db", "vod")
                    .data("scBCate", scBCate)
                    .cookies(session.cookies()).get();
            Element table = pageDoc.getElementById("NB_ListTable");
            Elements postRows = table.getElementsByTag("tr");
            for (Element postRow:postRows) {
                if (from <= postIndex && postIndex <= to) {
                    Elements postLinktd = postRow.getElementsByClass("tdPad4L6px");
                    if (postLinktd.get(0).getElementsByTag("a").size() > 0) {
                        String postUrl = postLinktd.get(0).getElementsByTag("a").get(0).attr("href");
                        fileUrls.addAll(getFileUrl(postUrl));
                    }
                }
                postIndex--;
            }
        }
        return fileUrls;
    }

    public int boardPagesNum(Document pageDoc) {
        String info = pageDoc.getElementsByClass("NB_tPageArea").get(0).text();
        int startIndex = info.indexOf("/")+1;
        int endIndex = info.indexOf("page");
        return Integer.parseInt(info.substring(startIndex, endIndex).strip());
    }

    public int boardPostsNum(Document pageDoc) {
        String info = pageDoc.getElementsByClass("NB_tPageArea").get(0).text();
        int startIndex = info.indexOf(":")+1;
        int endIndex = info.indexOf("건");
        return Integer.parseInt(info.substring(startIndex, endIndex).strip());
    }

    public ArrayList<String> getFileUrl(String postUrl) throws IOException {
        Document postDoc = Jsoup.connect(postUrl).cookies(session.cookies()).get();
        Element infoTable = postDoc.getElementById("NB_FormTable");
        Elements infoRows = infoTable.getElementsByTag("tr");
        ArrayList<String> fileUrls = new ArrayList<>();
        for (Element infoRow:infoRows) {
            Elements label = infoRow.getElementsByClass("nbLabelField pad");
            if (label.size() > 0) {
                if (label.get(0).text().contains("첨부파일")) {
                    Elements links = infoRow.getElementsByTag("a");
                    for (Element link:links) {
                        fileUrls.add(link.attr("href"));
                    }
                }
            }
        }
        return fileUrls;
    }
}