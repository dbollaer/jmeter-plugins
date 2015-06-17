package kg.apc.jmeter.regex;

import kg.apc.jmeter.JMeterPluginsUtils;
import org.apache.jmeter.extractor.*;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.JLabeledTextField;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Created by dbollaer on 5/28/15.
 */
public class RegexExtractorGui extends org.apache.jmeter.extractor.gui.RegexExtractorGui {

    @Override
    public String getStaticLabel() {
        return JMeterPluginsUtils.prefixLabel("Uber Regex");
    }

    private static final long serialVersionUID = 240L;
    private JLabeledTextField regexField;
    private JLabeledTextField templateField;
    private JLabeledTextField defaultField;
    private JLabeledTextField matchNumberField;
    private JLabeledTextField refNameField;
    private JLabeledTextField regexReplaceNameField;
    private JRadioButton useBody;
    private JRadioButton useUnescapedBody;
    private JRadioButton useBodyAsDocument;
    private JRadioButton useHeaders;
    private JRadioButton useURL;
    private JRadioButton useCode;
    private JRadioButton useMessage;
    private ButtonGroup group;

    public RegexExtractorGui() {
        this.init();
    }

    public String getLabelResource() {
        return "regex_extractor_title";
    }

    public void configure(TestElement el) {
        super.configure(el);
        if(el instanceof RegexExtractor) {
            RegexExtractor re = (RegexExtractor)el;
            this.showScopeSettings(re, true);
            this.useHeaders.setSelected(re.useHeaders());
            this.useBody.setSelected(re.useBody());
            this.useUnescapedBody.setSelected(re.useUnescapedBody());
            this.useBodyAsDocument.setSelected(re.useBodyAsDocument());
            this.useURL.setSelected(re.useUrl());
            this.useCode.setSelected(re.useCode());
            this.useMessage.setSelected(re.useMessage());
            this.regexField.setText(re.getRegex());
            this.templateField.setText(re.getTemplate());
            this.defaultField.setText(re.getDefaultValue());
            this.matchNumberField.setText(re.getMatchNumberAsString());
            this.refNameField.setText(re.getRefName());
            this.regexReplaceNameField.setText(re.getRegexRef());
        }

    }

    public TestElement createTestElement() {
        RegexExtractor extractor = new RegexExtractor();
        this.modifyTestElement(extractor);
        return extractor;
    }

    public void modifyTestElement(TestElement extractor) {
        super.configureTestElement(extractor);
        if(extractor instanceof RegexExtractor) {
            RegexExtractor regex = (RegexExtractor)extractor;
            this.saveScopeSettings(regex);
            regex.setUseField(this.group.getSelection().getActionCommand());
            regex.setRefName(this.refNameField.getText());
            regex.setRegex(this.regexField.getText());
            regex.setTemplate(this.templateField.getText());
            regex.setDefaultValue(this.defaultField.getText());
            regex.setMatchNumber(this.matchNumberField.getText());
            regex.setRegexRef(this.regexReplaceNameField.getText());
        }

    }

    public void clearGui() {
        super.clearGui();
        this.useBody.setSelected(true);
        this.regexField.setText("");
        this.templateField.setText("");
        this.defaultField.setText("");
        this.refNameField.setText("");
        this.matchNumberField.setText("");
        this.regexReplaceNameField.setText("");
    }

    private void init() {
        this.setLayout(new BorderLayout());
        this.setBorder(this.makeBorder());
        Box box = Box.createVerticalBox();
        box.add(this.makeTitlePanel());
        box.add(this.createScopePanel(true));
        box.add(this.makeSourcePanel());
        this.add(box, "North");
        this.add(this.makeParameterPanel(), "Center");
    }

    private JPanel makeSourcePanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(JMeterUtils.getResString("regex_source")));
        this.useBody = new JRadioButton(JMeterUtils.getResString("regex_src_body"));
        this.useUnescapedBody = new JRadioButton(JMeterUtils.getResString("regex_src_body_unescaped"));
        this.useBodyAsDocument = new JRadioButton(JMeterUtils.getResString("regex_src_body_as_document"));
        this.useHeaders = new JRadioButton(JMeterUtils.getResString("regex_src_hdrs"));
        this.useURL = new JRadioButton(JMeterUtils.getResString("regex_src_url"));
        this.useCode = new JRadioButton(JMeterUtils.getResString("assertion_code_resp"));
        this.useMessage = new JRadioButton(JMeterUtils.getResString("assertion_message_resp"));
        this.group = new ButtonGroup();
        this.group.add(this.useBody);
        this.group.add(this.useUnescapedBody);
        this.group.add(this.useBodyAsDocument);
        this.group.add(this.useHeaders);
        this.group.add(this.useURL);
        this.group.add(this.useCode);
        this.group.add(this.useMessage);
        panel.add(this.useBody);
        panel.add(this.useUnescapedBody);
        panel.add(this.useBodyAsDocument);
        panel.add(this.useHeaders);
        panel.add(this.useURL);
        panel.add(this.useCode);
        panel.add(this.useMessage);
        this.useBody.setSelected(true);
        this.useBody.setActionCommand("false");
        this.useUnescapedBody.setActionCommand("unescaped");
        this.useBodyAsDocument.setActionCommand("as_document");
        this.useHeaders.setActionCommand("true");
        this.useURL.setActionCommand("URL");
        this.useCode.setActionCommand("code");
        this.useMessage.setActionCommand("message");
        return panel;
    }

    private JPanel makeParameterPanel() {
        this.regexField = new JLabeledTextField(JMeterUtils.getResString("regex_field"));
        this.templateField = new JLabeledTextField(JMeterUtils.getResString("template_field"));
        this.defaultField = new JLabeledTextField(JMeterUtils.getResString("default_value_field"));
        this.refNameField = new JLabeledTextField(JMeterUtils.getResString("ref_name_field"));
        this.matchNumberField = new JLabeledTextField(JMeterUtils.getResString("match_num_field"));
        this.regexReplaceNameField = new JLabeledTextField("regex ref");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        this.initConstraints(gbc);
        this.addField(panel, this.refNameField, gbc);
        this.resetContraints(gbc);
        this.addField(panel, this.regexField, gbc);
        this.resetContraints(gbc);
        this.addField(panel, this.templateField, gbc);
        this.resetContraints(gbc);
        this.addField(panel, this.matchNumberField, gbc);
        this.resetContraints(gbc);
        gbc.weighty = 1.0D;
        this.addField(panel, this.defaultField, gbc);
        this.resetContraints(gbc);
        this.addField(panel, this.regexReplaceNameField, gbc);
        return panel;
    }

    private void addField(JPanel panel, JLabeledTextField field, GridBagConstraints gbc) {
        java.util.List item = field.getComponentList();
        panel.add((Component)item.get(0), gbc.clone());
        ++gbc.gridx;
        gbc.weightx = 1.0D;
        gbc.fill = 2;
        panel.add((Component)item.get(1), gbc.clone());
    }

    private void resetContraints(GridBagConstraints gbc) {
        gbc.gridx = 0;
        ++gbc.gridy;
        gbc.weightx = 0.0D;
        gbc.fill = 0;
    }

    private void initConstraints(GridBagConstraints gbc) {
        gbc.anchor = 18;
        gbc.fill = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0D;
        gbc.weighty = 0.0D;
    }
}
