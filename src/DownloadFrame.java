import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class DownloadFrame {
    JFrame frame;
    JTextField urlField;
    JTextField folderField;
    JTextField fromField;
    JTextField toField;
    JTextField idField;
    JPasswordField pwField;
    JCheckBox includeNameBox;
    JPanel downloadPanel;
    JLabel messageLabel;

    static FlowLayout leftLayout = new FlowLayout(FlowLayout.LEFT);

    public DownloadFrame() {
        frame = new JFrame();
        frame.setSize(800, 400);
        frame.setLayout(new GridLayout(10, 1, 0, 3));
        frame.add(createUrlPanel());
        frame.add(createFolderPanel());
        frame.add(createIndexPanel());
        frame.add(createIdPwPanel());
        frame.add(createIncludeNamePanel());
        downloadPanel = createDownloadPanel();
        frame.add(downloadPanel);
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

    private JPanel createIncludeNamePanel() {
        JPanel includeNamePanel = new JPanel(leftLayout);
        JLabel explainLabel = new JLabel("작성자를 파일명에 포함: ");
        includeNamePanel.add(explainLabel);
        includeNameBox = new JCheckBox();
        includeNamePanel.add(includeNameBox);
        return includeNamePanel;
    }

    private JPanel createDownloadPanel() {
        JPanel downloadPanel = new JPanel(leftLayout);
        JButton downloadButton = new JButton("다운로드");
        downloadButton.addActionListener(actionEvent -> {
            String scBCate;
            String folderPath;
            int from, to;
            String id, pw;
            boolean includeName;
            if (urlField.getText().contains("http")) {
                scBCate = getScBCate(urlField.getText());
            } else {
                scBCate = getScBCate("http://" + urlField.getText());
            }
            folderPath = folderField.getText();
            if (fromField.getText().equals("")) {
                from = 0;
            } else {
                from = Integer.parseInt(fromField.getText());
            }
            if (toField.getText().equals("")) {
                to = 0;
            } else {
                to = Integer.parseInt(toField.getText());
            }
            id = idField.getText();
            pw = String.valueOf(pwField.getPassword());
            includeName = includeNameBox.isSelected();
            DownloadTask downloadTask = new DownloadTask(scBCate, folderPath, from, to, id, pw, includeName, messageLabel);
            downloadTask.execute();
        });
        downloadPanel.add(downloadButton);
        messageLabel = new JLabel("");
        downloadPanel.add(messageLabel);
        return downloadPanel;
    }

    private String getScBCate(String urlString) {
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
}

class DownloadTask extends SwingWorker<String, String> {
    String scBCate;
    String folderPath;
    int from, to;
    String id, pw;
    JLabel messageLabel;
    boolean includeName;

    int downloadSuccessNum;

    Connection.Response session;

    static final String mainUrl = "http://lms.ksa.hs.kr";
    static final String loginUrl = mainUrl + "/Source/Include/login_ok.php";
    static final String boardUrl = mainUrl + "/nboard.php";

    public DownloadTask(String scBCate, String folderPath, int from, int to, String id, String pw,
                        boolean includeName, JLabel messageLabel) {
        this.scBCate = scBCate;
        this.folderPath = folderPath;
        this.from = from;
        this.to = to;
        this.id = id;
        this.pw = pw;
        this.messageLabel = messageLabel;
        this.includeName = includeName;
    }

    @Override
    protected String doInBackground() throws Exception {
        publish("로그인 중...");
        session = loginSession();
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
        downloadSuccessNum = 0;
        int startPage = (postsNum-to)/postsInPage+1;
        int endPage = (postsNum-from)/postsInPage+1;
        Document pageDoc;
        String poster;
        for (int page=startPage, postIndex=postsNum-(startPage-1)*postsInPage; page<=endPage; page++) {
            pageDoc = Jsoup.connect(boardUrl).data("page", Integer.toString(page)).data("db", "vod")
                    .data("scBCate", scBCate)
                    .cookies(session.cookies()).get();
            Element table = pageDoc.getElementById("NB_ListTable");
            Elements tbody = table.getElementsByTag("tbody");
            Elements postRows = tbody.get(0).getElementsByTag("tr");
            for (Element postRow:postRows) {
                poster = postRow.getElementsByClass("Board").get(0).text();
                if (from <= postIndex && postIndex <= to) {
                    publish(String.format("다운로드 중... %d/%d", to-postIndex+1, to-from+1));
                    Elements postLinktd = postRow.getElementsByClass("tdPad4L6px");
                    if (postLinktd.get(0).getElementsByTag("a").size() > 0) {
                        String postUrl = postLinktd.get(0).getElementsByTag("a").get(0).attr("href");
                        downloadPost(mainUrl + postUrl, poster);
                        downloadSuccessNum++;
                    }
                }
                postIndex--;
            }
        }
        int downloadFailedNum = (to-from+1)-downloadSuccessNum;
        String dialog;
        if (downloadFailedNum == 0) {
            publish("");
        } else {
            publish(String.format("%d개 다운로드 실패", downloadFailedNum));
        }
        JOptionPane.showMessageDialog(null, "다운로드 완료");
        return null;
    }

    @Override
    protected void process(List<String> chunks) {
        for (String chuck:chunks) {
            messageLabel.setText(chuck);
        }
    }

    private Connection.Response loginSession() throws IOException {
        return Jsoup.connect(loginUrl).data("user_id", id).data("user_pwd", pw)
                .method(Connection.Method.POST).execute();
    }

    private int boardPagesNum(Document pageDoc) {
        String info = pageDoc.getElementsByClass("NB_tPageArea").get(0).text();
        int startIndex = info.indexOf("/")+1;
        int endIndex = info.indexOf("page");
        return Integer.parseInt(info.substring(startIndex, endIndex).strip());
    }

    private int boardPostsNum(Document pageDoc) {
        String info = pageDoc.getElementsByClass("NB_tPageArea").get(0).text();
        int startIndex = info.indexOf(":")+1;
        int endIndex = info.indexOf("건");
        return Integer.parseInt(info.substring(startIndex, endIndex).strip());
    }

    private void downloadPost(String postUrl, String poster) throws IOException  {
        Document postDoc = Jsoup.connect(postUrl).cookies(session.cookies()).get();
        Element infoTable = postDoc.getElementById("NB_FormTable");
        Elements infoRows = infoTable.getElementsByTag("tr");
        String text;
        int startIndex, endIndex;
        for (Element infoRow:infoRows) {
            Elements label = infoRow.getElementsByClass("nbLabelField pad");
            if (label.size() > 0) {
                if (label.get(0).text().contains("첨부파일")) {
                    Elements links = infoRow.getElementsByTag("a");
                    for (Element link:links) {
                        String fileName = link.text();
                        fileName = fileName.substring(0, fileName.lastIndexOf("(")).strip();
                        if (includeName) {
                            fileName = String.format("(%s)%s", poster, fileName);
                        }
                        HttpDownloadUtility.downloadFile(mainUrl + link.attr("href"), folderPath, fileName);
                    }
                }
            }
        }
    }
}