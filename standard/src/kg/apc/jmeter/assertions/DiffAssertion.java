package kg.apc.jmeter.assertions;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.assertions.Assertion;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractScopedAssertion;
import org.apache.jmeter.testelement.property.*;
import org.apache.jmeter.util.Document;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.apache.oro.text.MalformedCachePatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

import javax.sound.midi.Patch;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by dbollaer on 5/27/15.
 */
public class DiffAssertion extends AbstractScopedAssertion implements Serializable, Assertion {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final long serialVersionUID = 240L;
    private static final String TEST_FIELD = "Assertion.test_field";
    private static final String SAMPLE_URL = "Assertion.sample_label";
    private static final String RESPONSE_DATA = "Assertion.response_data";
    private static final String RESPONSE_DATA_AS_DOCUMENT = "Assertion.response_data_as_document";
    private static final String RESPONSE_CODE = "Assertion.response_code";
    private static final String RESPONSE_MESSAGE = "Assertion.response_message";
    private static final String RESPONSE_HEADERS = "Assertion.response_headers";
    private static final String ASSUME_SUCCESS = "Assertion.assume_success";
    private static final String TEST_STRINGS = "Asserion.test_strings";
    private static final String TEST_TYPE = "Assertion.test_type";
    private static final int MATCH = 1;
    private static final int CONTAINS = 2;
    private static final int NOT = 4;
    private static final int EQUALS = 8;
    private static final int SUBSTRING = 16;
    private static final int TYPE_MASK = 27;
    private static final int EQUALS_SECTION_DIFF_LEN = JMeterUtils.getPropDefault("assertion.equals_section_diff_len", 100);
    private static final String EQUALS_DIFF_TRUNC = "...";
    private static final String RECEIVED_STR = "****** received  : ";
    private static final String COMPARISON_STR = "****** comparison: ";
    private static final String DIFF_DELTA_START = JMeterUtils.getPropDefault("assertion.equals_diff_delta_start", "[[[");
    private static final String DIFF_DELTA_END = JMeterUtils.getPropDefault("assertion.equals_diff_delta_end", "]]]");

    public DiffAssertion() {
        this.setProperty(new CollectionProperty("Asserion.test_strings", new ArrayList()));
    }

    public void clear() {
        super.clear();
        this.setProperty(new CollectionProperty("Asserion.test_strings", new ArrayList()));
    }

    private void setTestField(String testField) {
        this.setProperty("Assertion.test_field", testField);
    }

    public void setTestFieldURL() {
        this.setTestField("Assertion.sample_label");
    }

    public void setTestFieldResponseCode() {
        this.setTestField("Assertion.response_code");
    }

    public void setTestFieldResponseData() {
        this.setTestField("Assertion.response_data");
    }

    public void setTestFieldResponseDataAsDocument() {
        this.setTestField("Assertion.response_data_as_document");
    }

    public void setTestFieldResponseMessage() {
        this.setTestField("Assertion.response_message");
    }

    public void setTestFieldResponseHeaders() {
        this.setTestField("Assertion.response_headers");
    }

    public boolean isTestFieldURL() {
        return "Assertion.sample_label".equals(this.getTestField());
    }

    public boolean isTestFieldResponseCode() {
        return "Assertion.response_code".equals(this.getTestField());
    }

    public boolean isTestFieldResponseData() {
        return "Assertion.response_data".equals(this.getTestField());
    }

    public boolean isTestFieldResponseDataAsDocument() {
        return "Assertion.response_data_as_document".equals(this.getTestField());
    }

    public boolean isTestFieldResponseMessage() {
        return "Assertion.response_message".equals(this.getTestField());
    }

    public boolean isTestFieldResponseHeaders() {
        return "Assertion.response_headers".equals(this.getTestField());
    }

    private void setTestType(int testType) {
        this.setProperty(new IntegerProperty("Assertion.test_type", testType));
    }

    private void setTestTypeMasked(int testType) {
        int value = this.getTestType() & -28 | testType;
        this.setProperty(new IntegerProperty("Assertion.test_type", value));
    }

    public void addTestString(String testString) {
        this.getTestStrings().addProperty(new StringProperty(String.valueOf(testString.hashCode()), testString));
    }

    public void clearTestStrings() {
        this.getTestStrings().clear();
    }

    public AssertionResult getResult(SampleResult response) {
        AssertionResult result = this.evaluateResponse(response);
        return result;
    }

    public String getTestField() {
        return this.getPropertyAsString("Assertion.test_field");
    }

    public int getTestType() {
        JMeterProperty type = this.getProperty("Assertion.test_type");
        return type instanceof NullProperty ?2:type.getIntValue();
    }

    public CollectionProperty getTestStrings() {
        return (CollectionProperty)this.getProperty("Asserion.test_strings");
    }

    public boolean isEqualsType() {
        return (this.getTestType() & 8) != 0;
    }

    public boolean isSubstringType() {
        return (this.getTestType() & 16) != 0;
    }

    public boolean isContainsType() {
        return (this.getTestType() & 2) != 0;
    }

    public boolean isMatchType() {
        return (this.getTestType() & 1) != 0;
    }

    public boolean isNotType() {
        return (this.getTestType() & 4) != 0;
    }

    public void setToContainsType() {
        this.setTestTypeMasked(2);
    }

    public void setToMatchType() {
        this.setTestTypeMasked(1);
    }

    public void setToEqualsType() {
        this.setTestTypeMasked(8);
    }

    public void setToSubstringType() {
        this.setTestTypeMasked(16);
    }

    public void setToNotType() {
        this.setTestType(this.getTestType() | 4);
    }

    public void unsetNotType() {
        this.setTestType(this.getTestType() & -5);
    }

    public boolean getAssumeSuccess() {
        return this.getPropertyAsBoolean("Assertion.assume_success", false);
    }

    public void setAssumeSuccess(boolean b) {
        this.setProperty("Assertion.assume_success", b);
    }

    private AssertionResult evaluateResponse(SampleResult response) {
        AssertionResult result = new AssertionResult(this.getName());
        String toCheck = "";
        if(this.getAssumeSuccess()) {
            response.setSuccessful(true);
        }

        if(this.isScopeVariable()) {
            toCheck = this.getThreadContext().getVariables().get(this.getVariableName());
        } else if(this.isTestFieldResponseData()) {
            toCheck = response.getResponseDataAsString();
        } else if(this.isTestFieldResponseDataAsDocument()) {
            toCheck = Document.getTextFromDocument(response.getResponseData());
        } else if(this.isTestFieldResponseCode()) {
            toCheck = response.getResponseCode();
        } else if(this.isTestFieldResponseMessage()) {
            toCheck = response.getResponseMessage();
        } else if(this.isTestFieldResponseHeaders()) {
            toCheck = response.getResponseHeaders();
        } else {
            toCheck = "";
            URL notTest = response.getURL();
            if(notTest != null) {
                toCheck = notTest.toString();
            }
        }

        result.setFailure(false);
        result.setError(false);
        boolean notTest1 = (4 & this.getTestType()) > 0;
        boolean contains = this.isContainsType();
        boolean equals = this.isEqualsType();
        boolean substring = this.isSubstringType();
        boolean matches = this.isMatchType();
        boolean debugEnabled = log.isDebugEnabled();
        if(debugEnabled) {
            log.debug("Type:" + (contains?"Contains":"Match") + (notTest1?"(not)":""));
        }

        if(StringUtils.isEmpty(toCheck)) {
            if(notTest1) {
                return result;
            } else {
                if(debugEnabled) {
                    log.debug("Not checking empty response field in: " + response.getSampleLabel());
                }

                return result.setResultForNull();
            }
        } else {
            boolean pass = true;

            try {
                Perl5Matcher e = JMeterUtils.getMatcher();
                PropertyIterator iter = this.getTestStrings().iterator();

                while(iter.hasNext()) {
                    String stringPattern = iter.next().getStringValue();
                    Pattern pattern = null;
                    if(contains || matches) {
                        pattern = JMeterUtils.getPatternCache().getPattern(stringPattern, '耀');
                    }

                    boolean found;
                    if(contains) {

                        StringUtils.difference(toCheck,stringPattern);
                        found = e.contains(toCheck, pattern);
                    } else if(equals) {
                        found = toCheck.equals(stringPattern);
                    } else if(substring) {
                        found = toCheck.indexOf(stringPattern) != -1;
                    } else {
                        found = e.matches(toCheck, pattern);
                    }

                    pass = notTest1?!found:found;
                    if(!pass) {
                        if(debugEnabled) {
                            log.debug("Failed: " + stringPattern);
                        }

                        result.setFailure(true);
                        result.setFailureMessage(this.getFailText(stringPattern, toCheck));
                        break;
                    }

                    if(debugEnabled) {
                        log.debug("Passed: " + stringPattern);
                    }
                }
            } catch (MalformedCachePatternException var16) {
                result.setError(true);
                result.setFailure(false);
                result.setFailureMessage("Bad test configuration " + var16);
            }

            return result;
        }
    }

    private String getFailText(String stringPattern, String toCheck) {
        StringBuilder sb = new StringBuilder(200);
        sb.append("Test failed: ");
        if(this.isScopeVariable()) {
            sb.append("variable(").append(this.getVariableName()).append(')');
        } else if(this.isTestFieldResponseData()) {
            sb.append("text");
        } else if(this.isTestFieldResponseCode()) {
            sb.append("code");
        } else if(this.isTestFieldResponseMessage()) {
            sb.append("message");
        } else if(this.isTestFieldResponseHeaders()) {
            sb.append("headers");
        } else if(this.isTestFieldResponseDataAsDocument()) {
            sb.append("document");
        } else {
            sb.append("URL");
        }

        switch(this.getTestType()) {
            case 1:
                sb.append(" expected to match ");
                break;
            case 2:
            case 16:
                sb.append(" expected to contain ");
                break;
            case 3:
            case 4:
            case 7:
            case 9:
            case 10:
            case 11:
            case 13:
            case 14:
            case 15:
            case 17:
            case 18:
            case 19:
            default:
                sb.append(" expected something using ");
                break;
            case 5:
                sb.append(" expected not to match ");
                break;
            case 6:
            case 20:
                sb.append(" expected not to contain ");
                break;
            case 8:
                sb.append(" expected to equal ");
                break;
            case 12:
                sb.append(" expected not to equal ");
        }

        sb.append("/");
        if(this.isEqualsType()) {
            sb.append(equalsComparisonText(toCheck, stringPattern));
        } else {
            sb.append(stringPattern);
        }

        sb.append("/");
        return sb.toString();
    }

    private static String trunc(boolean right, String str) {
        return str.length() <= EQUALS_SECTION_DIFF_LEN?str:(right?str.substring(0, EQUALS_SECTION_DIFF_LEN) + "...":"..." + str.substring(str.length() - EQUALS_SECTION_DIFF_LEN, str.length()));
    }

    private static StringBuilder equalsComparisonText(String received, String comparison) {
        boolean lastRecDiff = true;
        boolean lastCompDiff = true;
        int recLength = received.length();
        int compLength = comparison.length();
        int minLength = Math.min(recLength, compLength);
        String recDeltaSeq = "";
        String compDeltaSeq = "";
        String endingEqSeq = "";
        StringBuilder text = new StringBuilder(Math.max(recLength, compLength) * 2);

        int firstDiff;
        for(firstDiff = 0; firstDiff < minLength && received.charAt(firstDiff) == comparison.charAt(firstDiff); ++firstDiff) {
            ;
        }

        String startingEqSeq;
        if(firstDiff == 0) {
            startingEqSeq = "";
        } else {
            startingEqSeq = trunc(false, received.substring(0, firstDiff));
        }

        int var15 = recLength - 1;

        int var16;
        for(var16 = compLength - 1; var15 > firstDiff && var16 > firstDiff && received.charAt(var15) == comparison.charAt(var16); --var16) {
            --var15;
        }

        endingEqSeq = trunc(true, received.substring(var15 + 1, recLength));
        if(endingEqSeq.length() == 0) {
            recDeltaSeq = trunc(true, received.substring(firstDiff, recLength));
            compDeltaSeq = trunc(true, comparison.substring(firstDiff, compLength));
        } else {
            recDeltaSeq = trunc(true, received.substring(firstDiff, var15 + 1));
            compDeltaSeq = trunc(true, comparison.substring(firstDiff, var16 + 1));
        }

        StringBuilder pad = new StringBuilder(Math.abs(recDeltaSeq.length() - compDeltaSeq.length()));

        for(int i = 0; i < pad.capacity(); ++i) {
            pad.append(' ');
        }

        if(recDeltaSeq.length() > compDeltaSeq.length()) {
            compDeltaSeq = compDeltaSeq + pad.toString();
        } else {
            recDeltaSeq = recDeltaSeq + pad.toString();
        }

        text.append("\n\n");
        text.append("****** received  : ");
        text.append(startingEqSeq);
        text.append(DIFF_DELTA_START);
        text.append(recDeltaSeq);
        text.append(DIFF_DELTA_END);
        text.append(endingEqSeq);
        text.append("\n\n");
        text.append("****** comparison: ");
        text.append(startingEqSeq);
        text.append(DIFF_DELTA_START);
        text.append(compDeltaSeq);
        text.append(DIFF_DELTA_END);
        text.append(endingEqSeq);
        text.append("\n\n");
        return text;
    }
}
