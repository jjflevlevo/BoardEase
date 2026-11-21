package boardinghouse.gui;

import boardinghouse.dao.AnnouncementDAO;
import boardinghouse.model.Announcement;
import javax.swing.*;
import java.awt.*;
import java.util.List;

// tenant: all announcements from the owner, newest first
public class AnnouncementFeed extends Section {
    private final AnnouncementDAO dao = new AnnouncementDAO();
    private final JPanel list = new JPanel();

    public AnnouncementFeed() {
        setBorder(BorderFactory.createEmptyBorder());
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(list, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(top, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(Ui.panelCard(Theme.title("Announcements"), scroll), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        list.removeAll();
        List<Announcement> all = dao.findAll();
        if (all.isEmpty()) {
            JLabel empty = Theme.muted("No announcements yet. Notices from the owner will appear here.");
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(empty);
        }
        for (Announcement a : all) {
            list.add(card(a));
            list.add(Box.createVerticalStrut(12));
        }
        list.revalidate();
        list.repaint();
    }

    private static JPanel card(Announcement a) {
        RoundedPanel card = new RoundedPanel(Theme.SIDEBAR, a.isImportant() ? Theme.DANGER : Theme.BORDER, 10, 16);
        card.setLayout(new BorderLayout(0, 8));

        JLabel title = new JLabel(a.getTitle());
        title.setFont(Theme.font(12, Font.BOLD));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        if (a.isImportant()) {
            JLabel tag = new JLabel("IMPORTANT");
            tag.setFont(Theme.font(8.5f, Font.BOLD));
            tag.setForeground(Theme.DANGER);
            right.add(tag);
        }
        right.add(Theme.muted("Posted " + Ui.localDate(a.getCreatedAt())));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(title, BorderLayout.WEST);
        head.add(right, BorderLayout.EAST);

        JLabel body = new JLabel("<html><body style='width:520px'>"
                + Theme.esc(a.getMessage()).replace("\n", "<br>") + "</body></html>");
        body.setFont(Theme.font(10.5f, Font.PLAIN));

        card.add(head, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }
}