package com.y5neko;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import com.alibaba.fastjson2.*;

public class XiaofenGuiViewer extends JFrame {
    private JTextField classifyField;
    private JPanel contentPanel;
    private JButton loadMoreBtn;

    private int currentPage = 1;
    private int totalPage = 1;

    public XiaofenGuiViewer() {
        setTitle("小粉圈破解版 v0.1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        gbc.gridx = 0;
        topPanel.add(new JLabel("手机号:"), gbc);
        gbc.gridx++;
        JTextField phoneField = new JTextField(ConfigUtil.loadUsername(), 10);
        topPanel.add(phoneField, gbc);
        gbc.gridx++;
        topPanel.add(new JLabel("密码:"), gbc);
        gbc.gridx++;
        JPasswordField passField = new JPasswordField(ConfigUtil.loadPassword(), 10);
        topPanel.add(passField, gbc);
        gbc.gridx++;
        JButton loginBtn = new JButton("登录");
        topPanel.add(loginBtn, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        topPanel.add(new JLabel("圈子id:"), gbc);
        gbc.gridx++;
        classifyField = new JTextField(ConfigUtil.loadID(), 10);
        topPanel.add(classifyField, gbc);
        gbc.gridx += 2;
        JButton fetchBtn = new JButton("获取数据");
        topPanel.add(fetchBtn, gbc);

        loginBtn.addActionListener(ev -> {
            String phone = phoneField.getText().trim();
            String pass = new String(passField.getPassword());
            doLogin(phone, pass);
        });
        fetchBtn.addActionListener(this::startFetch);

        // ===================中间内容
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private void startFetch(ActionEvent e) {
        ConfigUtil.saveID(classifyField.getText().trim());
        currentPage = 1;
        totalPage = 1;
        contentPanel.removeAll();
        if (loadMoreBtn != null) {
            contentPanel.remove(loadMoreBtn);
            loadMoreBtn = null;
        }
        fetchData(currentPage);
    }

    private void fetchData(int page) {
        String grassId = classifyField.getText().trim();
        new Thread(() -> {
            String token = ConfigUtil.loadToken();
            if (token == null || token.isEmpty()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "请先登录！"));
                return;
            }

            try {
                URL url = new URL("https://app.xiaofen.fun/api/m8551/64448e18f032c");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept-Platform", "Android");
                conn.setRequestProperty("user-token", token);
                conn.setRequestProperty("Accept-Language", "zh-Hans");
                conn.setRequestProperty("user-agent", "Mozilla/5.0 (Linux; Android 13; M2012K11C...)");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("page", page);
                body.put("pagesize", 10);
                body.put("list_rows", 10);
                body.put("sort", 1);
                body.put("is_payment", 5);
                body.put("type", 2);
                body.put("grassclassify_id", Integer.parseInt(grassId));
                body.put("user_id", 532173);
                body.put("ven", 134);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toJSONString().getBytes());
                }

                InputStream in = conn.getInputStream();
                String response = new BufferedReader(new InputStreamReader(in))
                        .lines().collect(Collectors.joining("\n"));
                parseAndDisplay(response);

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "请求失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void parseAndDisplay(String response) {
        SwingUtilities.invokeLater(() -> {
            try {
                JSONObject json = JSON.parseObject(response);
                JSONObject dataObj = json.getJSONObject("data");
                JSONArray items = dataObj.getJSONArray("data");

                int total = dataObj.getIntValue("total");
                totalPage = (int) Math.ceil(total / 10.0);

                for (int i = 0; i < items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);

                    JPanel itemPanel = new JPanel();

                    itemPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(1, 1, 4, 1, new Color(180, 180, 180)),
                            new EmptyBorder(10, 10, 10, 10)
                    ));
                    itemPanel.setBackground(new Color(250, 250, 250));

                    String title = item.getString("title");
                    String introduce = item.getString("introduce");
                    if (title == null) {
                        title = "无标题";
                    }
                    if (introduce == null) {
                        introduce = "无内容";
                    }

                    JLabel titleLabel = new JLabel("<html><b>" + title + "</b></html>");
                    JLabel introLabel = new JLabel("<html>" + introduce + "</html>");
                    JPanel mediaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

                    String video = item.getString("video");
                    if (video != null && !video.isEmpty()) {
                        // 先显示“视频预览生成中...”
                        JLabel loadingLabel = new JLabel("视频预览生成中...");
                        mediaPanel.add(loadingLabel);

                        // 异步生成缩略图并显示
                        generateVideoThumbnailAsync(video, mediaPanel);
                    } else {
                        JSONArray images = item.getJSONArray("images_url");
                        if (images != null) {
                            for (int j = 0; j < images.size(); j++) {
                                String imgUrl = images.getString(j);
                                JLabel imgLabel = new JLabel();
                                imgLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                mediaPanel.add(imgLabel);

                                // 异步加载图片缩略图并设置
                                new Thread(() -> {
                                    try {
                                        ImageIcon icon = new ImageIcon(new URL(imgUrl));
                                        Image rawImage = icon.getImage();
                                        int w = rawImage.getWidth(null);
                                        int h = rawImage.getHeight(null);
                                        int maxW = 150;
                                        int maxH = 150;

                                        float ratio = Math.min((float) maxW / w, (float) maxH / h);
                                        int displayW = (int) (w * ratio);
                                        int displayH = (int) (h * ratio);

                                        Image scaled = rawImage.getScaledInstance(displayW, displayH, Image.SCALE_SMOOTH);
                                        ImageIcon scaledIcon = new ImageIcon(scaled);

                                        SwingUtilities.invokeLater(() -> {
                                            imgLabel.setIcon(scaledIcon);
                                            imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                                                public void mouseClicked(java.awt.event.MouseEvent evt) {
                                                    openImagePreviewDialog(imgUrl);
                                                }
                                            });
                                        });
                                    } catch (Exception e) {
                                        SwingUtilities.invokeLater(() -> {
                                            imgLabel.setText("加载失败");
                                        });
                                    }
                                }).start();
                            }
                        }
                    }

                    itemPanel.add(titleLabel);
                    itemPanel.add(introLabel);
                    itemPanel.add(mediaPanel);
                    contentPanel.add(itemPanel);
                }

                // 移除旧按钮，避免按钮重复
                if (loadMoreBtn != null) {
                    contentPanel.remove(loadMoreBtn);
                }

                // 如果还有下一页，显示加载更多按钮
                if (currentPage < totalPage) {
                    loadMoreBtn = new JButton("加载更多");
                    loadMoreBtn.addActionListener(ev -> {
                        currentPage++;
                        fetchData(currentPage);
                    });
                    contentPanel.add(loadMoreBtn);
                } else {
                    loadMoreBtn = null; // 不显示按钮
                }

                contentPanel.revalidate();
                contentPanel.repaint();

            } catch (Exception e) {
                e.printStackTrace();
                contentPanel.add(new JLabel("解析失败: " + e.getMessage()));
            }
        });
    }

    private void generateVideoThumbnailAsync(String videoUrl, JPanel mediaPanel) {
        new Thread(() -> {
            try {
                File thumbFile = generateThumbnail(videoUrl);
                if (thumbFile != null && thumbFile.exists()) {
                    ImageIcon icon = new ImageIcon(thumbFile.getAbsolutePath());
                    Image rawImage = icon.getImage();
                    int w = rawImage.getWidth(null);
                    int h = rawImage.getHeight(null);
                    int maxW = 200, maxH = 150;
                    float ratio = Math.min((float) maxW / w, (float) maxH / h);
                    int displayW = (int) (w * ratio);
                    int displayH = (int) (h * ratio);
                    Image scaled = rawImage.getScaledInstance(displayW, displayH, Image.SCALE_SMOOTH);
                    ImageIcon scaledIcon = new ImageIcon(scaled);

                    SwingUtilities.invokeLater(() -> {
                        mediaPanel.removeAll();
                        JComponent videoComp = createVideoThumbnailComponent(scaledIcon, videoUrl);
                        mediaPanel.add(videoComp);
                        mediaPanel.revalidate();
                        mediaPanel.repaint();
                    });
                } else {
                    fallbackVideoButton(mediaPanel, videoUrl);
                }
            } catch (Exception e) {
                fallbackVideoButton(mediaPanel, videoUrl);
            }
        }).start();
    }

    private void fallbackVideoButton(JPanel mediaPanel, String videoUrl) {
        SwingUtilities.invokeLater(() -> {
            mediaPanel.removeAll();
            JButton videoBtn = new JButton("播放视频");
            videoBtn.addActionListener(ev -> openInBrowser(videoUrl));
            mediaPanel.add(videoBtn);
            mediaPanel.revalidate();
            mediaPanel.repaint();
        });
    }

    private File generateThumbnail(String videoUrl) throws Exception {
        // 下载视频到临时文件
        File tempVideoFile = File.createTempFile("temp_video", ".mp4");
        tempVideoFile.deleteOnExit();
        try (InputStream in = new URL(videoUrl).openStream();
             OutputStream out = new FileOutputStream(tempVideoFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        // 生成缩略图文件
        File thumbnailFile = Files.createTempFile("video_thumbnail", ".jpg").toFile();
        thumbnailFile.deleteOnExit();

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", tempVideoFile.getAbsolutePath(),
                "-ss", "00:00:01",
                "-vframes", "1",
                thumbnailFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 执行失败，退出码: " + exitCode);
        }

        return thumbnailFile;
    }

    private JComponent createVideoThumbnailComponent(ImageIcon thumbnail, String videoUrl) {
        int w = thumbnail.getIconWidth();
        int h = thumbnail.getIconHeight();

        JLabel thumbLabel = new JLabel(thumbnail);
        thumbLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel playIcon = new JLabel("\u25B6"); // ▶
        playIcon.setForeground(new Color(255, 0, 0, 180)); // 半透明红
        playIcon.setFont(new Font("SansSerif", Font.BOLD, Math.min(w, h) / 2));
        playIcon.setHorizontalAlignment(SwingConstants.CENTER);
        playIcon.setVerticalAlignment(SwingConstants.CENTER);
        playIcon.setBounds(0, 0, w, h);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(w, h));
        thumbLabel.setBounds(0, 0, w, h);
        layeredPane.add(thumbLabel, Integer.valueOf(0));
        layeredPane.add(playIcon, Integer.valueOf(1));

        layeredPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                openInBrowser(videoUrl);
            }
        });

        return layeredPane;
    }

//    private void openImagePreviewDialog(String imageUrl) {
//        JDialog dialog = new JDialog(this, "图片预览", true);
//        try {
//            ImageIcon icon = new ImageIcon(new URL(imageUrl));
//            Image rawImg = icon.getImage();
//
//            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//            int maxW = screenSize.width - 100;
//            int maxH = screenSize.height - 100;
//
//            int w = rawImg.getWidth(null);
//            int h = rawImg.getHeight(null);
//
//            float ratio = Math.min((float) maxW / w, (float) maxH / h);
//            if (ratio > 1) ratio = 1f; // 不放大
//
//            int displayW = (int) (w * ratio);
//            int displayH = (int) (h * ratio);
//
//            Image scaled = rawImg.getScaledInstance(displayW, displayH, Image.SCALE_SMOOTH);
//            JLabel label = new JLabel(new ImageIcon(scaled));
//
//            JScrollPane scrollPane = new JScrollPane(label);
//            scrollPane.setPreferredSize(new Dimension(displayW, displayH));
//
//            dialog.getContentPane().add(scrollPane);
//            dialog.pack();
//            dialog.setLocationRelativeTo(this);
//            dialog.setVisible(true);
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, "无法加载图片：" + e.getMessage());
//        }
//    }

    private void openImagePreviewDialog(String imageUrl) {
        JDialog dialog = new JDialog(this, "图片预览", true);
        try {
            ImageIcon icon = new ImageIcon(new URL(imageUrl));
            Image rawImg = icon.getImage();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int maxW = screenSize.width - 100;
            int maxH = screenSize.height - 100;

            int w = rawImg.getWidth(null);
            int h = rawImg.getHeight(null);

            float ratio = Math.min((float) maxW / w, (float) maxH / h);
            if (ratio > 1) ratio = 1f;

            int displayW = (int) (w * ratio);
            int displayH = (int) (h * ratio);

            Image scaled = rawImg.getScaledInstance(displayW, displayH, Image.SCALE_SMOOTH);
            JLabel label = new JLabel(new ImageIcon(scaled));

            JScrollPane scrollPane = new JScrollPane(label);
            scrollPane.setPreferredSize(new Dimension(displayW, displayH));

            // 👉 新增按钮面板
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton openInBrowserBtn = new JButton("在浏览器中打开");
            openInBrowserBtn.addActionListener(e -> openInBrowser(imageUrl));
            btnPanel.add(openInBrowserBtn);

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.add(scrollPane, BorderLayout.CENTER);
            wrapper.add(btnPanel, BorderLayout.SOUTH);

            dialog.getContentPane().add(wrapper);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "无法加载图片：" + e.getMessage());
        }
    }


    private void openInBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URL(url).toURI());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "无法打开链接: " + url);
        }
    }

    private void doLogin(String phone, String password) {
        new Thread(() -> {
            try {
                URL url = new URL("https://app.xiaofen.fun/api/m8551/5c78dbfd977cf");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept-Platform", "Android");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("account", phone);
                body.put("account_type", "mobile");
                body.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toJSONString().getBytes());
                }

                InputStream in = conn.getInputStream();
                String response = new BufferedReader(new InputStreamReader(in))
                        .lines().collect(Collectors.joining("\n"));

                JSONObject json = JSON.parseObject(response);
                if ("1".equals(json.getString("code"))) {
                    String token = json.getJSONObject("data").getJSONObject("userinfo").getString("user_token");
                    ConfigUtil.saveToken(token, phone, password);
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "登录成功，已保存 token"));
                } else {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "登录失败：" + json.getString("msg")));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "登录异常：" + ex.getMessage()));
            }
        }).start();
    }


    public static void main(String[] args) {
        if (!Files.exists(Paths.get(ConfigUtil.rootPath))) {
            File dir = new File(ConfigUtil.rootPath);
            dir.mkdir();
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new XiaofenGuiViewer().setVisible(true));
    }
}
