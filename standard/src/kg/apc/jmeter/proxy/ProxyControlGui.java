package kg.apc.jmeter.proxy;

import kg.apc.jmeter.JMeterPluginsUtils;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.gui.TreeNodeWrapper;
import org.apache.jmeter.engine.util.ValueReplacer;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.gui.util.HeaderAsPropertyRenderer;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.protocol.http.proxy.*;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerFactory;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.testelement.WorkBench;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.GuiUtils;
import org.apache.jorphan.gui.JLabeledTextField;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.BindException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dbollaer on 5/20/15.
 */
public class ProxyControlGui extends org.apache.jmeter.protocol.http.proxy.gui.ProxyControlGui {

    @Override
    public String getStaticLabel() {
        return JMeterPluginsUtils.prefixLabel("Dummy Proxy");
    }


    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final long serialVersionUID = 232L;
    private static final String NEW_LINE = "\n";
    private static final String SPACE = " ";
    private static final String USE_DEFAULT_HTTP_IMPL = "";
    private static final String SUGGESTED_EXCLUSIONS = JMeterUtils.getPropDefault("proxy.excludes.suggested", ".*\\.(bmp|css|js|gif|ico|jpe?g|png|swf|woff)");
    private JTextField portField;
    private JLabeledTextField sslDomains;
    private JCheckBox httpHeaders;
    private JComboBox groupingMode;
    private JCheckBox addAssertions;
    private JCheckBox useKeepAlive;
    private JCheckBox regexMatch;
    private JComboBox samplerTypeName;
    private JCheckBox samplerRedirectAutomatically;
    private JCheckBox samplerFollowRedirects;
    private JCheckBox samplerDownloadImages;
    private JTextField contentTypeInclude;
    private JTextField contentTypeExclude;
    private JComboBox targetNodes;
    private DefaultComboBoxModel targetNodesModel;
    private ProxyControl model;
    private JTable excludeTable;
    private PowerTableModel excludeModel;
    private JTable includeTable;
    private PowerTableModel includeModel;
    private static final String CHANGE_TARGET = "change_target";
    private JButton stop;
    private JButton start;
    private JButton restart;
    private static final String STOP = "stop";
    private static final String START = "start";
    private static final String RESTART = "restart";
    private static final String ENABLE_RESTART = "enable_restart";
    private static final String ADD_INCLUDE = "add_include";
    private static final String ADD_EXCLUDE = "add_exclude";
    private static final String DELETE_INCLUDE = "delete_include";
    private static final String DELETE_EXCLUDE = "delete_exclude";
    private static final String ADD_TO_INCLUDE_FROM_CLIPBOARD = "include_clipboard";
    private static final String ADD_TO_EXCLUDE_FROM_CLIPBOARD = "exclude_clipboard";
    private static final String ADD_SUGGESTED_EXCLUDES = "exclude_suggested";
    private static final String INCLUDE_COL = "patterns_to_include";
    private static final String EXCLUDE_COL = "patterns_to_exclude";
    private static final String PORTFIELD = "portField";

    public ProxyControlGui() {
        log.debug("Creating ProxyControlGui");
        this.init();
    }

    public TestElement createTestElement() {
        this.model = this.makeProxyControl();
        log.debug("creating/configuring model = " + this.model);
        this.modifyTestElement(this.model);
        return this.model;
    }

    protected ProxyControl makeProxyControl() {
        ProxyControl local = new ProxyControlDummy();
        return local;
    }

    public void modifyTestElement(TestElement el) {
        GuiUtils.stopTableEditing(this.excludeTable);
        GuiUtils.stopTableEditing(this.includeTable);
        this.configureTestElement(el);
        if(el instanceof ProxyControl) {
            this.model = (ProxyControl)el;
            this.model.setPort(this.portField.getText());
            this.model.setSslDomains(this.sslDomains.getText());
            this.setIncludeListInProxyControl(this.model);
            this.setExcludeListInProxyControl(this.model);
            this.model.setCaptureHttpHeaders(this.httpHeaders.isSelected());
            this.model.setGroupingMode(this.groupingMode.getSelectedIndex());
            this.model.setAssertions(this.addAssertions.isSelected());
            if(this.samplerTypeName.getSelectedIndex() < HTTPSamplerFactory.getImplementations().length) {
                this.model.setSamplerTypeName(HTTPSamplerFactory.getImplementations()[this.samplerTypeName.getSelectedIndex()]);
            } else {
                this.model.setSamplerTypeName("");
            }

            this.model.setSamplerRedirectAutomatically(this.samplerRedirectAutomatically.isSelected());
            this.model.setSamplerFollowRedirects(this.samplerFollowRedirects.isSelected());
            this.model.setUseKeepAlive(this.useKeepAlive.isSelected());
            this.model.setSamplerDownloadImages(this.samplerDownloadImages.isSelected());
            this.model.setRegexMatch(this.regexMatch.isSelected());
            this.model.setContentTypeInclude(this.contentTypeInclude.getText());
            this.model.setContentTypeExclude(this.contentTypeExclude.getText());
            TreeNodeWrapper nw = (TreeNodeWrapper)this.targetNodes.getSelectedItem();
            if(nw == null) {
                this.model.setTarget((JMeterTreeNode)null);
            } else {
                this.model.setTarget(nw.getTreeNode());
            }
        }

    }

    protected void setIncludeListInProxyControl(org.apache.jmeter.protocol.http.proxy.ProxyControl element) {
        List includeList = this.getDataList(this.includeModel, "patterns_to_include");
        element.setIncludeList(includeList);
    }

    protected void setExcludeListInProxyControl(org.apache.jmeter.protocol.http.proxy.ProxyControl element) {
        List excludeList = this.getDataList(this.excludeModel, "patterns_to_exclude");
        element.setExcludeList(excludeList);
    }

    private List<String> getDataList(PowerTableModel p_model, String colName) {
        String[] dataArray = p_model.getData().getColumn(colName);
        LinkedList list = new LinkedList();

        for(int i = 0; i < dataArray.length; ++i) {
            list.add(dataArray[i]);
        }

        return list;
    }

    public String getLabelResource() {
        return "proxy_title";
    }

    public Collection<String> getMenuCategories() {
        return Arrays.asList(new String[]{"menu_non_test_elements"});
    }

    public void configure(TestElement element) {
        log.debug("Configuring gui with " + element);
        super.configure(element);
        this.model = (ProxyControl)element;
        this.portField.setText(this.model.getPortString());
        this.sslDomains.setText(this.model.getSslDomains());
        this.httpHeaders.setSelected(this.model.getCaptureHttpHeaders());
        this.groupingMode.setSelectedIndex(this.model.getGroupingMode());
        this.addAssertions.setSelected(this.model.getAssertions());
        this.samplerTypeName.setSelectedItem(this.model.getSamplerTypeName());
        this.samplerRedirectAutomatically.setSelected(this.model.getSamplerRedirectAutomatically());
        this.samplerFollowRedirects.setSelected(this.model.getSamplerFollowRedirects());
        this.useKeepAlive.setSelected(this.model.getUseKeepalive());
        this.samplerDownloadImages.setSelected(this.model.getSamplerDownloadImages());
        this.regexMatch.setSelected(this.model.getRegexMatch());
        this.contentTypeInclude.setText(this.model.getContentTypeInclude());
        this.contentTypeExclude.setText(this.model.getContentTypeExclude());
        this.reinitializeTargetCombo();
        this.populateTable(this.includeModel, this.model.getIncludePatterns().iterator());
        this.populateTable(this.excludeModel, this.model.getExcludePatterns().iterator());
        this.repaint();
    }

    private void populateTable(PowerTableModel p_model, PropertyIterator iter) {
        p_model.clearData();

        while(iter.hasNext()) {
            p_model.addRow(new Object[]{iter.next().getStringValue()});
        }

        p_model.fireTableDataChanged();
    }

    public void itemStateChanged(ItemEvent e) {
        this.enableRestart();
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        Object source = action.getSource();
        if(source.equals(this.samplerFollowRedirects) && this.samplerFollowRedirects.isSelected()) {
            this.samplerRedirectAutomatically.setSelected(false);
        } else if(source.equals(this.samplerRedirectAutomatically) && this.samplerRedirectAutomatically.isSelected()) {
            this.samplerFollowRedirects.setSelected(false);
        }

        if(command.equals("stop")) {
            this.model.stopProxy();
            this.stop.setEnabled(false);
            this.start.setEnabled(true);
            this.restart.setEnabled(false);
        } else if(command.equals("start")) {
            this.startProxy();
        } else if(command.equals("restart")) {
            this.model.stopProxy();
            this.startProxy();
        } else if(command.equals("enable_restart")) {
            this.enableRestart();
        } else if(command.equals("add_exclude")) {
            this.excludeModel.addNewRow();
            this.excludeModel.fireTableDataChanged();
            this.enableRestart();
        } else if(command.equals("add_include")) {
            this.includeModel.addNewRow();
            this.includeModel.fireTableDataChanged();
            this.enableRestart();
        } else if(command.equals("delete_exclude")) {
            this.excludeModel.removeRow(this.excludeTable.getSelectedRow());
            this.excludeModel.fireTableDataChanged();
            this.enableRestart();
        } else if(command.equals("delete_include")) {
            this.includeModel.removeRow(this.includeTable.getSelectedRow());
            this.includeModel.fireTableDataChanged();
            this.enableRestart();
        } else if(command.equals("change_target")) {
            log.debug("Change target " + this.targetNodes.getSelectedItem());
            log.debug("In model " + this.model);
            TreeNodeWrapper nw = (TreeNodeWrapper)this.targetNodes.getSelectedItem();
            this.model.setTarget(nw.getTreeNode());
            this.enableRestart();
        } else if(command.equals("include_clipboard")) {
            this.addFromClipboard(this.includeTable);
            this.includeModel.fireTableDataChanged();
            this.enableRestart();
        } else if(command.equals("exclude_clipboard")) {
            this.addFromClipboard(this.excludeTable);
            this.excludeModel.fireTableDataChanged();
            this.enableRestart();
        } else if(command.equals("exclude_suggested")) {
            this.addSuggestedExcludes(this.excludeTable);
            this.excludeModel.fireTableDataChanged();
            this.enableRestart();
        }

    }

    protected void addSuggestedExcludes(JTable table) {
        GuiUtils.stopTableEditing(table);
        int rowCount = table.getRowCount();
        PowerTableModel model = null;
        String[] exclusions = SUGGESTED_EXCLUSIONS.split(";");
        if(exclusions.length > 0) {
            model = (PowerTableModel)table.getModel();
            if(model != null) {
                String[] rowToSelect = exclusions;
                int len$ = exclusions.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    String clipboardLine = rowToSelect[i$];
                    model.addRow(new Object[]{clipboardLine});
                }

                if(table.getRowCount() > rowCount) {
                    int var9 = model.getRowCount() - 1;
                    table.setRowSelectionInterval(rowCount, var9);
                }
            }
        }

    }

    protected void addFromClipboard(JTable table) {
        GuiUtils.stopTableEditing(table);
        int rowCount = table.getRowCount();
        PowerTableModel model = null;

        try {
            String ufe = GuiUtils.getPastedText();
            if(ufe != null) {
                String[] clipboardLines = ufe.split("\n");
                String[] rowToSelect = clipboardLines;
                int len$ = clipboardLines.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    String clipboardLine = rowToSelect[i$];
                    model = (PowerTableModel)table.getModel();
                    model.addRow(new Object[]{clipboardLine});
                }

                if(table.getRowCount() > rowCount && model != null) {
                    int var12 = model.getRowCount() - 1;
                    table.setRowSelectionInterval(rowCount, var12);
                }
            }
        } catch (IOException var10) {
            JOptionPane.showMessageDialog(this, JMeterUtils.getResString("proxy_daemon_error_read_args") + "\n" + var10.getLocalizedMessage(), JMeterUtils.getResString("error_title"), 0);
        } catch (UnsupportedFlavorException var11) {
            JOptionPane.showMessageDialog(this, JMeterUtils.getResString("proxy_daemon_error_not_retrieve") + " " + DataFlavor.stringFlavor.getHumanPresentableName() + " " + JMeterUtils.getResString("proxy_daemon_error_from_clipboard") + var11.getLocalizedMessage(), JMeterUtils.getResString("error_title"), 0);
        }

    }

    private void startProxy() {
        ValueReplacer replacer = GuiPackage.getInstance().getReplacer();
        this.modifyTestElement(this.model);
        Cursor cursor = this.getCursor();
        this.setCursor(Cursor.getPredefinedCursor(3));

        try {
            replacer.replaceValues(this.model);
            this.model.startProxy();
            this.start.setEnabled(false);
            this.stop.setEnabled(true);
            this.restart.setEnabled(false);
            if(org.apache.jmeter.protocol.http.proxy.ProxyControl.isDynamicMode()) {
                String[] e = this.model.getCertificateDetails();
                StringBuilder sb = new StringBuilder();
                sb.append(JMeterUtils.getResString("proxy_daemon_msg_rootca_cert")).append(" ").append("ApacheJMeterTemporaryRootCA").append(" ").append(JMeterUtils.getResString("proxy_daemon_msg_created_in_bin"));
                sb.append("\n").append(JMeterUtils.getResString("proxy_daemon_msg_install_as_in_doc"));
                sb.append("\n").append(JMeterUtils.getResString("proxy_daemon_msg_check_details")).append("\n").append("\n");
                String[] arr$ = e;
                int len$ = e.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    String detail = arr$[i$];
                    sb.append(detail).append("\n");
                }

                JOptionPane.showMessageDialog(this, sb.toString(), JMeterUtils.getResString("proxy_daemon_msg_rootca_cert") + " " + "ApacheJMeterTemporaryRootCA" + " " + JMeterUtils.getResString("proxy_daemon_msg_created_in_bin"), 1);
            }
        } catch (InvalidVariableException var14) {
            JOptionPane.showMessageDialog(this, JMeterUtils.getResString("invalid_variables") + ": " + var14.getMessage(), JMeterUtils.getResString("error_title"), 0);
        } catch (BindException var15) {
            JOptionPane.showMessageDialog(this, JMeterUtils.getResString("proxy_daemon_bind_error") + ": " + var15.getMessage(), JMeterUtils.getResString("error_title"), 0);
        } catch (IOException var16) {
            JOptionPane.showMessageDialog(this, JMeterUtils.getResString("proxy_daemon_error") + ": " + var16.getMessage(), JMeterUtils.getResString("error_title"), 0);
        } finally {
            this.setCursor(cursor);
        }

    }

    private void enableRestart() {
        if(this.stop.isEnabled()) {
            this.restart.setEnabled(true);
        }

    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
        String fieldName = e.getComponent().getName();
        if(fieldName.equals("portField")) {
            try {
                Integer.parseInt(this.portField.getText());
            } catch (NumberFormatException var5) {
                int length = this.portField.getText().length();
                if(length > 0) {
                    JOptionPane.showMessageDialog(this, JMeterUtils.getResString("proxy_settings_port_error_digits"), JMeterUtils.getResString("proxy_settings_port_error_invalid_data"), 2);
                    this.portField.setText(this.portField.getText().substring(0, length - 1));
                }
            }

            this.enableRestart();
        } else if(fieldName.equals("enable_restart")) {
            this.enableRestart();
        }

    }

    private void init() {
        this.setLayout(new BorderLayout(0, 5));
        this.setBorder(this.makeBorder());
        this.add(this.makeTitlePanel(), "North");
        JPanel mainPanel = new JPanel(new BorderLayout());
        Box myBox = Box.createVerticalBox();
        myBox.add(this.createPortPanel());
        myBox.add(Box.createVerticalStrut(5));
        myBox.add(this.createTestPlanContentPanel());
        myBox.add(Box.createVerticalStrut(5));
        myBox.add(this.createHTTPSamplerPanel());
        myBox.add(Box.createVerticalStrut(5));
        myBox.add(this.createContentTypePanel());
        myBox.add(Box.createVerticalStrut(5));
        mainPanel.add(myBox, "North");
        Box includeExcludePanel = Box.createVerticalBox();
        includeExcludePanel.add(this.createIncludePanel());
        includeExcludePanel.add(this.createExcludePanel());
        mainPanel.add(includeExcludePanel, "Center");
        mainPanel.add(this.createControls(), "South");
        this.add(mainPanel, "Center");
    }

    private JPanel createControls() {
        this.start = new JButton(JMeterUtils.getResString("start"));
        this.start.addActionListener(this);
        this.start.setActionCommand("start");
        this.start.setEnabled(true);
        this.stop = new JButton(JMeterUtils.getResString("stop"));
        this.stop.addActionListener(this);
        this.stop.setActionCommand("stop");
        this.stop.setEnabled(false);
        this.restart = new JButton(JMeterUtils.getResString("restart"));
        this.restart.addActionListener(this);
        this.restart.setActionCommand("restart");
        this.restart.setEnabled(false);
        JPanel panel = new JPanel();
        panel.add(this.start);
        panel.add(this.stop);
        panel.add(this.restart);
        return panel;
    }

    private JPanel createPortPanel() {
        this.portField = new JTextField(org.apache.jmeter.protocol.http.proxy.ProxyControl.DEFAULT_PORT_S, 5);
        this.portField.setName("portField");
        this.portField.addKeyListener(this);
        JLabel label = new JLabel(JMeterUtils.getResString("port"));
        label.setLabelFor(this.portField);
        JPanel gPane = new JPanel(new BorderLayout());
        gPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), JMeterUtils.getResString("proxy_general_settings")));
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label);
        panel.add(this.portField);
        panel.add(Box.createHorizontalStrut(10));
        gPane.add(panel, "West");
        this.sslDomains = new JLabeledTextField(JMeterUtils.getResString("proxy_domains"));
        this.sslDomains.setEnabled(org.apache.jmeter.protocol.http.proxy.ProxyControl.isDynamicMode());
        if(org.apache.jmeter.protocol.http.proxy.ProxyControl.isDynamicMode()) {
            this.sslDomains.setToolTipText(JMeterUtils.getResString("proxy_domains_dynamic_mode_tooltip"));
        } else {
            this.sslDomains.setToolTipText(JMeterUtils.getResString("proxy_domains_dynamic_mode_tooltip_java6"));
        }

        gPane.add(this.sslDomains, "Center");
        return gPane;
    }

    private JPanel createTestPlanContentPanel() {
        this.httpHeaders = new JCheckBox(JMeterUtils.getResString("proxy_headers"));
        this.httpHeaders.setSelected(true);
        this.httpHeaders.addActionListener(this);
        this.httpHeaders.setActionCommand("enable_restart");
        this.addAssertions = new JCheckBox(JMeterUtils.getResString("proxy_assertions"));
        this.addAssertions.setSelected(false);
        this.addAssertions.addActionListener(this);
        this.addAssertions.setActionCommand("enable_restart");
        this.regexMatch = new JCheckBox(JMeterUtils.getResString("proxy_regex"));
        this.regexMatch.setSelected(false);
        this.regexMatch.addActionListener(this);
        this.regexMatch.setActionCommand("enable_restart");
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), JMeterUtils.getResString("proxy_test_plan_content")));
        HorizontalPanel nodeCreationPanel = new HorizontalPanel();
        nodeCreationPanel.add(this.createGroupingPanel());
        nodeCreationPanel.add(this.httpHeaders);
        nodeCreationPanel.add(this.addAssertions);
        nodeCreationPanel.add(this.regexMatch);
        HorizontalPanel targetPanel = new HorizontalPanel();
        targetPanel.add(this.createTargetPanel());
        mainPanel.add(targetPanel);
        mainPanel.add(nodeCreationPanel);
        return mainPanel;
    }

    private JPanel createHTTPSamplerPanel() {
        DefaultComboBoxModel m = new DefaultComboBoxModel();
        String[] label2 = HTTPSamplerFactory.getImplementations();
        int panel = label2.length;

        for(int i$ = 0; i$ < panel; ++i$) {
            String s = label2[i$];
            m.addElement(s);
        }

        m.addElement("");
        this.samplerTypeName = new JComboBox(m);
        this.samplerTypeName.setPreferredSize(new Dimension(150, 20));
        this.samplerTypeName.setSelectedItem("");
        this.samplerTypeName.addItemListener(this);
        JLabel var6 = new JLabel(JMeterUtils.getResString("proxy_sampler_type"));
        var6.setLabelFor(this.samplerTypeName);
        this.samplerRedirectAutomatically = new JCheckBox(JMeterUtils.getResString("follow_redirects_auto"));
        this.samplerRedirectAutomatically.setSelected(false);
        this.samplerRedirectAutomatically.addActionListener(this);
        this.samplerRedirectAutomatically.setActionCommand("enable_restart");
        this.samplerFollowRedirects = new JCheckBox(JMeterUtils.getResString("follow_redirects"));
        this.samplerFollowRedirects.setSelected(true);
        this.samplerFollowRedirects.addActionListener(this);
        this.samplerFollowRedirects.setActionCommand("enable_restart");
        this.useKeepAlive = new JCheckBox(JMeterUtils.getResString("use_keepalive"));
        this.useKeepAlive.setSelected(true);
        this.useKeepAlive.addActionListener(this);
        this.useKeepAlive.setActionCommand("enable_restart");
        this.samplerDownloadImages = new JCheckBox(JMeterUtils.getResString("web_testing_retrieve_images"));
        this.samplerDownloadImages.setSelected(false);
        this.samplerDownloadImages.addActionListener(this);
        this.samplerDownloadImages.setActionCommand("enable_restart");
        HorizontalPanel var7 = new HorizontalPanel();
        var7.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), JMeterUtils.getResString("proxy_sampler_settings")));
        var7.add(var6);
        var7.add(this.samplerTypeName);
        var7.add(this.samplerRedirectAutomatically);
        var7.add(this.samplerFollowRedirects);
        var7.add(this.useKeepAlive);
        var7.add(this.samplerDownloadImages);
        return var7;
    }

    private JPanel createTargetPanel() {
        this.targetNodesModel = new DefaultComboBoxModel();
        this.targetNodes = new JComboBox(this.targetNodesModel);
        this.targetNodes.setActionCommand("change_target");
        JLabel label = new JLabel(JMeterUtils.getResString("proxy_target"));
        label.setLabelFor(this.targetNodes);
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label);
        panel.add(this.targetNodes);
        return panel;
    }

    private JPanel createGroupingPanel() {
        DefaultComboBoxModel m = new DefaultComboBoxModel();
        m.addElement(JMeterUtils.getResString("grouping_no_groups"));
        m.addElement(JMeterUtils.getResString("grouping_add_separators"));
        m.addElement(JMeterUtils.getResString("grouping_in_controllers"));
        m.addElement(JMeterUtils.getResString("grouping_store_first_only"));
        m.addElement(JMeterUtils.getResString("grouping_in_transaction_controllers"));
        this.groupingMode = new JComboBox(m);
        this.groupingMode.setPreferredSize(new Dimension(150, 20));
        this.groupingMode.setSelectedIndex(0);
        this.groupingMode.addItemListener(this);
        JLabel label2 = new JLabel(JMeterUtils.getResString("grouping_mode"));
        label2.setLabelFor(this.groupingMode);
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label2);
        panel.add(this.groupingMode);
        return panel;
    }

    private JPanel createContentTypePanel() {
        this.contentTypeInclude = new JTextField(35);
        this.contentTypeInclude.addKeyListener(this);
        this.contentTypeInclude.setName("enable_restart");
        JLabel labelInclude = new JLabel(JMeterUtils.getResString("proxy_content_type_include"));
        labelInclude.setLabelFor(this.contentTypeInclude);
        this.contentTypeInclude.setText(JMeterUtils.getProperty("proxy.content_type_include"));
        this.contentTypeExclude = new JTextField(35);
        this.contentTypeExclude.addKeyListener(this);
        this.contentTypeExclude.setName("enable_restart");
        JLabel labelExclude = new JLabel(JMeterUtils.getResString("proxy_content_type_exclude"));
        labelExclude.setLabelFor(this.contentTypeExclude);
        this.contentTypeExclude.setText(JMeterUtils.getProperty("proxy.content_type_exclude"));
        HorizontalPanel panel = new HorizontalPanel();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), JMeterUtils.getResString("proxy_content_type_filter")));
        panel.add(labelInclude);
        panel.add(this.contentTypeInclude);
        panel.add(labelExclude);
        panel.add(this.contentTypeExclude);
        return panel;
    }

    private JPanel createIncludePanel() {
        this.includeModel = new PowerTableModel(new String[]{"patterns_to_include"}, new Class[]{String.class});
        this.includeTable = new JTable(this.includeModel);
        this.includeTable.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer());
        this.includeTable.setPreferredScrollableViewportSize(new Dimension(100, 30));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), JMeterUtils.getResString("patterns_to_include")));
        panel.add(new JScrollPane(this.includeTable), "Center");
        panel.add(this.createTableButtonPanel("add_include", "delete_include", "include_clipboard", (String)null), "South");
        return panel;
    }

    private JPanel createExcludePanel() {
        this.excludeModel = new PowerTableModel(new String[]{"patterns_to_exclude"}, new Class[]{String.class});
        this.excludeTable = new JTable(this.excludeModel);
        this.excludeTable.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer());
        this.excludeTable.setPreferredScrollableViewportSize(new Dimension(100, 30));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), JMeterUtils.getResString("patterns_to_exclude")));
        panel.add(new JScrollPane(this.excludeTable), "Center");
        panel.add(this.createTableButtonPanel("add_exclude", "delete_exclude", "exclude_clipboard", "exclude_suggested"), "South");
        return panel;
    }

    private JPanel createTableButtonPanel(String addCommand, String deleteCommand, String copyFromClipboard, String addSuggestedExcludes) {
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton(JMeterUtils.getResString("add"));
        addButton.setActionCommand(addCommand);
        addButton.addActionListener(this);
        buttonPanel.add(addButton);
        JButton deleteButton = new JButton(JMeterUtils.getResString("delete"));
        deleteButton.setActionCommand(deleteCommand);
        deleteButton.addActionListener(this);
        buttonPanel.add(deleteButton);
        JButton addFromClipboard = new JButton(JMeterUtils.getResString("add_from_clipboard"));
        addFromClipboard.setActionCommand(copyFromClipboard);
        addFromClipboard.addActionListener(this);
        buttonPanel.add(addFromClipboard);
        if(addSuggestedExcludes != null) {
            JButton addFromSuggestedExcludes = new JButton(JMeterUtils.getResString("add_from_suggested_excludes"));
            addFromSuggestedExcludes.setActionCommand(addSuggestedExcludes);
            addFromSuggestedExcludes.addActionListener(this);
            buttonPanel.add(addFromSuggestedExcludes);
        }

        return buttonPanel;
    }

    private void reinitializeTargetCombo() {
        log.debug("Reinitializing target combo");
        this.targetNodes.removeActionListener(this);
        this.targetNodesModel.removeAllElements();
        GuiPackage gp = GuiPackage.getInstance();
        if(gp != null) {
            JMeterTreeNode root = (JMeterTreeNode)GuiPackage.getInstance().getTreeModel().getRoot();
            this.targetNodesModel.addElement(new TreeNodeWrapper((JMeterTreeNode)null, JMeterUtils.getResString("use_recording_controller")));
            this.buildNodesModel(root, "", 0);
        }

        TreeNodeWrapper choice = null;

        for(int i = 0; i < this.targetNodesModel.getSize(); ++i) {
            choice = (TreeNodeWrapper)this.targetNodesModel.getElementAt(i);
            log.debug("Selecting item " + choice + " for model " + this.model + " in " + this);
            if(choice.getTreeNode() == this.model.getTarget()) {
                break;
            }
        }

        this.targetNodes.addActionListener(this);
        this.targetNodesModel.setSelectedItem(choice);
        log.debug("Reinitialization complete");
    }

    private void buildNodesModel(JMeterTreeNode node, String parent_name, int level) {
        String separator = " > ";
        if(node != null) {
            for(int i = 0; i < node.getChildCount(); ++i) {
                StringBuilder name = new StringBuilder();
                JMeterTreeNode cur = (JMeterTreeNode)node.getChildAt(i);
                TestElement te = cur.getTestElement();
                if(te instanceof Controller) {
                    name.append(parent_name);
                    name.append(cur.getName());
                    TreeNodeWrapper tnw = new TreeNodeWrapper(cur, name.toString());
                    this.targetNodesModel.addElement(tnw);
                    name.append(separator);
                    this.buildNodesModel(cur, name.toString(), level + 1);
                } else if(te instanceof TestPlan || te instanceof WorkBench) {
                    name.append(cur.getName());
                    name.append(separator);
                    this.buildNodesModel(cur, name.toString(), 0);
                }
            }
        }

    }

}
