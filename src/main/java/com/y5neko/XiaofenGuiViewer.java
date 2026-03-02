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

        // 添加窗口大小变化监听器，实现自适应布局
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                // 窗口大小改变时，重新计算所有帖子项的布局
                SwingUtilities.invokeLater(() -> {
                    updateLayoutOnResize();
                });
            }
        });
    }


    /**
     * 窗口大小变化时更新布局
     * 遍历所有帖子项，重新计算标题和描述的宽度，触发媒体面板重新布局
     */
    private void updateLayoutOnResize() {
        // 获取滚动面板的视口宽度
        Container parent = contentPanel.getParent();
        int containerWidth = 850;
        if (parent instanceof JViewport) {
            containerWidth = ((JViewport) parent).getWidth();
        } else if (contentPanel.getWidth() > 0) {
            containerWidth = contentPanel.getWidth();
        }
        
        if (containerWidth <= 0) {
            return;
        }

        // 遍历所有帖子项
        for (java.awt.Component comp : contentPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel itemPanel = (JPanel) comp;
                
                // 更新 itemPanel 的最大宽度
                itemPanel.setMaximumSize(new Dimension(containerWidth, Integer.MAX_VALUE));
                
                // 遍历帖子项中的组件
                for (java.awt.Component innerComp : itemPanel.getComponents()) {
                    // 更新标题和描述标签的宽度
                    if (innerComp instanceof JLabel) {
                        JLabel label = (JLabel) innerComp;
                        String text = label.getText();
                        if (text != null && text.startsWith("<html>")) {
                            // 更新 HTML div 的宽度
                            String newText = text.replaceAll("width:\\d+px", "width:" + (containerWidth - 40) + "px");
                            label.setText(newText);
                            label.setMaximumSize(new Dimension(containerWidth - 20, Integer.MAX_VALUE));
                        }
                    }
                    // 触发媒体面板重新布局
                    else if (innerComp instanceof JPanel) {
                        JPanel mediaPanel = (JPanel) innerComp;
                        // 更新媒体面板的最大宽度
                        mediaPanel.setMaximumSize(new Dimension(containerWidth - 20, Integer.MAX_VALUE));
                        mediaPanel.revalidate();
                        mediaPanel.repaint();
                    }
                }
                
                itemPanel.revalidate();
                itemPanel.repaint();
            }
        }
        
        contentPanel.revalidate();
        contentPanel.repaint();
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

                // 获取滚动面板的视口宽度（更准确的容器宽度）
                Container parent = contentPanel.getParent();
                int containerWidth = 850; // 默认宽度
                if (parent instanceof JViewport) {
                    containerWidth = ((JViewport) parent).getWidth();
                } else if (contentPanel.getWidth() > 0) {
                    containerWidth = contentPanel.getWidth();
                }

                for (int i = 0; i < items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);

                    // 使用垂直布局（BoxLayout.Y_AXIS），让每个元素独占一行
                    JPanel itemPanel = new JPanel();
                    itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));

                    itemPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(1, 1, 4, 1, new Color(180, 180, 180)),
                            new EmptyBorder(10, 10, 10, 10)
                    ));
                    itemPanel.setBackground(new Color(250, 250, 250));
                    
                    // 关键：限制 itemPanel 的最大宽度，防止超出容器
                    itemPanel.setMaximumSize(new Dimension(containerWidth, Integer.MAX_VALUE));

                    String title = item.getString("title");
                    String introduce = item.getString("introduce");
                    if (title == null) {
                        title = "无标题";
                    }
                    if (introduce == null) {
                        introduce = "无内容";
                    }

                    // 标题标签 - 第一行（限制宽度以支持换行）
                    JLabel titleLabel = new JLabel("<html><div style='width:" + (containerWidth - 40) + "px; word-wrap: break-word;'><b>" + title + "</b></div></html>");
                    titleLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                    titleLabel.setMaximumSize(new Dimension(containerWidth - 20, Integer.MAX_VALUE));
                    
                    // 描述标签 - 第二行（限制宽度以支持换行）
                    JLabel introLabel = new JLabel("<html><div style='width:" + (containerWidth - 40) + "px; word-wrap: break-word;'>" + introduce + "</div></html>");
                    introLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                    introLabel.setMaximumSize(new Dimension(containerWidth - 20, Integer.MAX_VALUE));
                    
                    // 媒体面板 - 第三行（使用自定义 WrapLayout 支持自动换行）
                    JPanel mediaPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 5, 5));
                    mediaPanel.setBackground(new Color(250, 250, 250));
                    mediaPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                    // 关键：限制媒体面板的最大宽度，使其能够自动换行
                    mediaPanel.setMaximumSize(new Dimension(containerWidth - 20, Integer.MAX_VALUE));

                    String video = item.getString("video");
                    if (video != null && !video.isEmpty()) {
                        // 先显示"视频预览生成中..."
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
                                            // 图片加载后重新计算布局
                                            mediaPanel.revalidate();
                                            mediaPanel.repaint();
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

                    // 按顺序添加组件（垂直排列）
                    itemPanel.add(titleLabel);
                    itemPanel.add(Box.createVerticalStrut(5)); // 添加垂直间距
                    itemPanel.add(introLabel);
                    itemPanel.add(Box.createVerticalStrut(5)); // 添加垂直间距
                    itemPanel.add(mediaPanel);
                    
                    contentPanel.add(itemPanel);
                    contentPanel.add(Box.createVerticalStrut(10)); // 列表项之间的间距
                }

                // 移除旧按钮，避免按钮重复
                if (loadMoreBtn != null) {
                    contentPanel.remove(loadMoreBtn);
                }

                // 如果还有下一页，显示加载更多按钮
                if (currentPage < totalPage) {
                    loadMoreBtn = new JButton("加载更多");
                    loadMoreBtn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                    loadMoreBtn.setMaximumSize(new Dimension(150, 30));
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


    /**
     * 自定义换行布局管理器
     * 扩展 FlowLayout，支持在指定宽度时自动换行
     */
    private static class WrapLayout extends FlowLayout {
        
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }
        
        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        @Override
        public void layoutContainer(Container target) {
            synchronized (target.getTreeLock()) {
                Insets insets = target.getInsets();
                int maxWidth = target.getWidth() - insets.left - insets.right - getHgap() * 2;
                
                int nmembers = target.getComponentCount();
                int x = insets.left + getHgap();
                int y = insets.top + getVgap();
                int rowHeight = 0;
                
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = m.getPreferredSize();
                        
                        // 检查是否需要换行
                        if (x + d.width > maxWidth + insets.left + getHgap() && x > insets.left + getHgap()) {
                            // 换行
                            y += rowHeight + getVgap();
                            x = insets.left + getHgap();
                            rowHeight = 0;
                        }
                        
                        m.setBounds(x, y, d.width, d.height);
                        x += d.width + getHgap();
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
            }
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                // 获取目标容器或其父容器的宽度
                int targetWidth = target.getWidth();
                
                // 如果容器宽度为0（初始状态），尝试获取父容器的宽度
                if (targetWidth <= 0) {
                    Container parent = target.getParent();
                    if (parent != null) {
                        targetWidth = parent.getWidth();
                        if (parent.getInsets() != null) {
                            targetWidth -= parent.getInsets().left + parent.getInsets().right;
                        }
                    }
                    if (targetWidth <= 0) {
                        targetWidth = 800; // 最终默认值
                    }
                }

                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right - getHgap() * 2;
                if (maxWidth <= 0) {
                    maxWidth = targetWidth - 40;
                }

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        
                        // 如果当前行放不下这个组件且不是行首，换行
                        if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                            dim.width = Math.max(dim.width, rowWidth);
                            dim.height += rowHeight + getVgap();
                            rowWidth = 0;
                            rowHeight = 0;
                        }

                        // 如果不是行首，添加水平间距
                        if (rowWidth > 0) {
                            rowWidth += getHgap();
                        }

                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }

                dim.width = Math.max(dim.width, rowWidth);
                dim.height += rowHeight;

                dim.width += insets.left + insets.right + getHgap() * 2;
                dim.height += insets.top + insets.bottom + getVgap() * 2;

                return dim;
            }
        }
    }
}
