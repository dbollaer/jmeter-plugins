package kg.apc.jmeter.regex;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractScopedTestElement;
import org.apache.jmeter.testelement.property.IntegerProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.Document;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.apache.oro.text.MalformedCachePatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Matcher;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by dbollaer on 5/27/15.
 */
public class RegexExtractor  extends org.apache.jmeter.extractor.RegexExtractor {

    private static final long serialVersionUID = 240L;
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final String MATCH_AGAINST = "RegexExtractor.useHeaders";
    public static final String USE_HDRS = "true";
    public static final String USE_BODY = "false";
    public static final String USE_BODY_UNESCAPED = "unescaped";
    public static final String USE_BODY_AS_DOCUMENT = "as_document";
    public static final String USE_URL = "URL";
    public static final String USE_CODE = "code";
    public static final String USE_MESSAGE = "message";
    private static final String REGEX = "RegexExtractor.regex";
    private static final String REFNAME = "RegexExtractor.refname";
    private static final String MATCH_NUMBER = "RegexExtractor.match_number";
    private static final String DEFAULT = "RegexExtractor.default";
    private static final String TEMPLATE = "RegexExtractor.template";
    private static final String REF_MATCH_NR = "_matchNr";
    private static final String UNDERSCORE = "_";
    private static final String REGEX_REF = "RegexExtractor.regex_ref";

    public String getFoundMatch() {
        return foundMatch;
    }

    private String foundMatch = "";
    private transient List<Object> template;

    public RegexExtractor() {
    }

    public boolean findMatches(SampleResult previousResult){
        this.initTemplate();

        boolean foundMatches = false;
        if(previousResult != null) {
            log.debug("RegexExtractor processing result");

            String refName = this.getRefName();
            int matchNumber = this.getMatchNumber();
            String defaultValue = this.getDefaultValue();


            Perl5Matcher matcher = JMeterUtils.getMatcher();
            String regex = this.getRegex();
            Pattern pattern = null;

            try {
                pattern = JMeterUtils.getPatternCache().getPattern(regex, '耀');
                List e = this.processMatchesGlobal(pattern, regex, previousResult, matchNumber);
                foundMatches = !(e.isEmpty());


            } catch (MalformedCachePatternException var24) {
                log.error("Error in pattern: " + regex);
            } finally {
                JMeterUtils.clearMatcherMemory(matcher, pattern);
            }

        }
        return foundMatches;
    }


    public ConcurrentHashMap<String,String> findMatches2(SampleResult previousResult){
        // this.initTemplate();
        this.initTemplate();
        ConcurrentHashMap<String,String> foundMatches = new ConcurrentHashMap<String,String>();

        if(previousResult != null) {
            log.debug("RegexExtractor processing result");

            String refName = this.getRefName();
            int matchNumber = this.getMatchNumber();


            Perl5Matcher matcher = JMeterUtils.getMatcher();
            String regex = this.getRegex();
            Pattern pattern = null;

            try {
                pattern = JMeterUtils.getPatternCache().getPattern(regex, '耀');
                List e = this.processMatchesGlobal(pattern, regex, previousResult, matchNumber);

                int matchCount = 0;

                try {
                    MatchResult e1;
                    int i;
                    String refName_n;
                    String result;
                    if(matchNumber >= 0) {
                        e1 = this.getCorrectMatch(e, matchNumber);
                        if(e1 != null) {
                            result =  this.generateResult(e1);
                            foundMatches.put(refName, result);
                            foundMatches.put("__urlencode(${" + refName + "})" , URLEncoder.encode(result));


                        }
                    } else {

                        matchCount = e.size();


                        for(i = 1; i <= matchCount; ++i) {
                            e1 = this.getCorrectMatch(e, i);
                            if(e1 != null) {
                                refName_n = refName + "_" + i;
                                result = this.generateResult(e1);
                                foundMatches.put(refName_n, result);
                                foundMatches.put("__urlencode(${" + refName_n + "})" , URLEncoder.encode(result));

                            }
                        }
                    }

                } catch (RuntimeException var23) {
                    log.warn("Error while generating result" + var23.getMessage());
                }

            } catch (MalformedCachePatternException var24) {
                log.error("Error in pattern2: " + regex);
            } finally {
                JMeterUtils.clearMatcherMemory(matcher, pattern);
            }

        }
        return foundMatches;
    }

    public void process() {
        this.initTemplate();
        JMeterContext context = this.getThreadContext();
        SampleResult previousResult = context.getPreviousResult();
        if(previousResult != null) {
            log.debug("RegexExtractor processing result");
            JMeterVariables vars = context.getVariables();
            String refName = this.getRefName();
            int matchNumber = this.getMatchNumber();
            String defaultValue = this.getDefaultValue();
            if(defaultValue.length() > 0) {
                vars.put(refName, defaultValue);
            }

            Perl5Matcher matcher = JMeterUtils.getMatcher();
            String regex = this.getRegex();
            Pattern pattern = null;

            try {
                pattern = JMeterUtils.getPatternCache().getPattern(regex, '耀');
                List e = this.processMatches(pattern, regex, previousResult, matchNumber, vars);

                int prevCount = 0;
                String prevString = vars.get(refName + "_matchNr");
                if(prevString != null) {
                    vars.remove(refName + "_matchNr");

                    try {
                        prevCount = Integer.parseInt(prevString);
                    } catch (NumberFormatException var22) {
                        log.warn("Could not parse " + prevString + " " + var22);
                    }
                }

                int matchCount = 0;

                try {
                    MatchResult e1;
                    int i;
                    String refName_n;
                    if(matchNumber >= 0) {
                        e1 = this.getCorrectMatch(e, matchNumber);
                        if(e1 != null) {
                            vars.put(refName, this.generateResult(e1));
                            this.saveGroups(vars, refName, e1);
                        } else {
                            this.removeGroups(vars, refName);
                        }
                    } else {
                        this.removeGroups(vars, refName);
                        matchCount = e.size();
                        vars.put(refName + "_matchNr", Integer.toString(matchCount));

                        for(i = 1; i <= matchCount; ++i) {
                            e1 = this.getCorrectMatch(e, i);
                            if(e1 != null) {
                                refName_n = refName + "_" + i;
                                vars.put(refName_n, this.generateResult(e1));
                                this.saveGroups(vars, refName_n, e1);
                            }
                        }
                    }

                    for(i = matchCount + 1; i <= prevCount; ++i) {
                        refName_n = refName + "_" + i;
                        vars.remove(refName_n);
                        this.removeGroups(vars, refName_n);
                    }
                } catch (RuntimeException var23) {
                    log.warn("Error while generating result");
                }
            } catch (MalformedCachePatternException var24) {
                log.error("Error in pattern: " + regex);
            } finally {
                JMeterUtils.clearMatcherMemory(matcher, pattern);
            }

        }
    }

    private String getInputString(SampleResult result) {
        String inputString = this.useUrl()?result.getUrlAsString():(this.useHeaders()?result.getResponseHeaders():(this.useCode()?result.getResponseCode():(this.useMessage()?result.getResponseMessage():(this.useUnescapedBody()? StringEscapeUtils.unescapeHtml4(result.getResponseDataAsString()):(this.useBodyAsDocument()? Document.getTextFromDocument(result.getResponseData()):result.getResponseDataAsString())))));
        if(log.isDebugEnabled()) {
            log.debug("Input = " + inputString);
        }

        return inputString;
    }

    private List<MatchResult> processMatches(Pattern pattern, String regex, SampleResult result, int matchNumber, JMeterVariables vars) {
        if(log.isDebugEnabled()) {
            log.debug("Regex = " + regex);
        }

        Perl5Matcher matcher = JMeterUtils.getMatcher();
        ArrayList matches = new ArrayList();
        int found = 0;
        if(this.isScopeVariable()) {
            String sampleList = vars.get(this.getVariableName());
            if(sampleList == null) {
                log.warn("No variable \'" + this.getVariableName() + "\' found to process by RegexExtractor \'" + this.getName() + "\', skipping processing");
                return Collections.emptyList();
            }

            this.matchStrings(matchNumber, matcher, pattern, matches, found, sampleList);
        } else {
            List sampleList1 = this.getSampleList(result);
            Iterator i$ = sampleList1.iterator();

            while(i$.hasNext()) {
                SampleResult sr = (SampleResult)i$.next();
                String inputString = this.getInputString(sr);
                found = this.matchStrings(matchNumber, matcher, pattern, matches, found, inputString);
                if(matchNumber > 0 && found == matchNumber) {
                    break;
                }
            }
        }

        return matches;
    }


    public List<MatchResult> processMatchesGlobal(Pattern pattern, String regex, SampleResult result, int matchNumber) {
        if(log.isDebugEnabled()) {
            log.debug("Regex = " + regex);
        }

        Perl5Matcher matcher = JMeterUtils.getMatcher();
        ArrayList matches = new ArrayList();
        int found = 0;

            List sampleList1 = this.getSampleList(result);
            Iterator i$ = sampleList1.iterator();

            while(i$.hasNext()) {
                SampleResult sr = (SampleResult)i$.next();
                String inputString = this.getInputString(sr);
                found = this.matchStrings(matchNumber, matcher, pattern, matches, found, inputString);
                if(matchNumber > 0 && found == matchNumber) {
                    break;
                }
            }


        return matches;
    }

    private int matchStrings(int matchNumber, Perl5Matcher matcher, Pattern pattern, List<MatchResult> matches, int found, String inputString) {
        for(PatternMatcherInput input = new PatternMatcherInput(inputString); (matchNumber <= 0 || found != matchNumber) && matcher.contains(input, pattern); ++found) {
            log.debug("RegexExtractor: Match found!");
            matches.add(matcher.getMatch());
        }

        return found;
    }

    private void saveGroups(JMeterVariables vars, String basename, MatchResult match) {
        StringBuilder buf = new StringBuilder();
        buf.append(basename);
        buf.append("_g");
        int pfxlen = buf.length();
        String prevString = vars.get(buf.toString());
        int previous = 0;
        if(prevString != null) {
            try {
                previous = Integer.parseInt(prevString);
            } catch (NumberFormatException var10) {
                log.warn("Could not parse " + prevString + " " + var10);
            }
        }

        int groups = match.groups();

        int i;
        for(i = 0; i < groups; ++i) {
            buf.append(i);
            vars.put(buf.toString(), match.group(i));
            buf.setLength(pfxlen);
        }

        vars.put(buf.toString(), Integer.toString(groups - 1));

        for(i = groups; i <= previous; ++i) {
            buf.append(i);
            vars.remove(buf.toString());
            buf.setLength(pfxlen);
        }

    }

    private void removeGroups(JMeterVariables vars, String basename) {
        StringBuilder buf = new StringBuilder();
        buf.append(basename);
        buf.append("_g");
        int pfxlen = buf.length();

        int groups;
        try {
            groups = Integer.parseInt(vars.get(buf.toString()));
        } catch (NumberFormatException var7) {
            groups = 0;
        }

        vars.remove(buf.toString());

        for(int i = 0; i <= groups; ++i) {
            buf.append(i);
            vars.remove(buf.toString());
            buf.setLength(pfxlen);
        }

    }

    private String generateResult(MatchResult match) {
        StringBuilder result = new StringBuilder();
        Iterator i$ = this.template.iterator();

        while(i$.hasNext()) {
            Object obj = i$.next();
            if(log.isDebugEnabled()) {
                log.debug("RegexExtractor: Template piece " + obj + " (" + obj.getClass().getSimpleName() + ")");
            }

            if(obj instanceof Integer) {
                result.append(match.group(((Integer)obj).intValue()));
            } else {
                result.append(obj);
            }
        }

        if(log.isDebugEnabled()) {
            log.debug("Regex Extractor result = " + result.toString());
        }

        return result.toString();
    }

    private void initTemplate() {
        if(this.template == null) {
            ArrayList combined = new ArrayList();
            String rawTemplate = this.getTemplate();
            Perl5Matcher matcher = JMeterUtils.getMatcher();
            Pattern templatePattern = JMeterUtils.getPatternCache().getPattern("\\$(\\d+)\\$", 0);
            if(log.isDebugEnabled()) {
                log.debug("Pattern = " + templatePattern.getPattern());
                log.debug("template = " + rawTemplate);
            }

            int beginOffset = 0;

            MatchResult currentResult;
            for(PatternMatcherInput pinput = new PatternMatcherInput(rawTemplate); matcher.contains(pinput, templatePattern); beginOffset = currentResult.endOffset(0)) {
                currentResult = matcher.getMatch();
                int i$ = currentResult.beginOffset(0);
                if(i$ > beginOffset) {
                    combined.add(rawTemplate.substring(beginOffset, i$));
                }

                combined.add(Integer.valueOf(currentResult.group(1)));
            }

            if(beginOffset < rawTemplate.length()) {
                combined.add(rawTemplate.substring(beginOffset, rawTemplate.length()));
            }

            if(log.isDebugEnabled()) {
                log.debug("Template item count: " + combined.size());
                Iterator i$1 = combined.iterator();

                while(i$1.hasNext()) {
                    Object o = i$1.next();
                    log.debug(o.getClass().getSimpleName() + " \'" + o.toString() + "\'");
                }
            }

            this.template = combined;
        }
    }

    private MatchResult getCorrectMatch(List<MatchResult> matches, int entry) {
        int matchSize = matches.size();
        return matchSize > 0 && entry <= matchSize?(entry == 0?(MatchResult)matches.get(JMeterUtils.getRandomInt(matchSize)):(MatchResult)matches.get(entry - 1)):null;
    }

    public void setRegexRef(String regexRef) {
        this.setProperty("RegexExtractor.regex_ref", regexRef);
    }

    public String getRegexRef() {
        return this.getPropertyAsString("RegexExtractor.regex_ref");
    }

    public void setRegex(String regex) {
        this.setProperty("RegexExtractor.regex", regex);
    }

    public String getRegex() {
        return this.getPropertyAsString("RegexExtractor.regex");
    }

    public void setRefName(String refName) {
        this.setProperty("RegexExtractor.refname", refName);
    }

    public String getRefName() {
        return this.getPropertyAsString("RegexExtractor.refname");
    }

    public void setMatchNumber(int matchNumber) {
        this.setProperty(new IntegerProperty("RegexExtractor.match_number", matchNumber));
    }

    public void setMatchNumber(String matchNumber) {
        this.setProperty("RegexExtractor.match_number", matchNumber);
    }

    public int getMatchNumber() {
        return this.getPropertyAsInt("RegexExtractor.match_number");
    }

    public String getMatchNumberAsString() {
        return this.getPropertyAsString("RegexExtractor.match_number");
    }

    public void setDefaultValue(String defaultValue) {
        this.setProperty("RegexExtractor.default", defaultValue);
    }

    public String getDefaultValue() {
        return this.getPropertyAsString("RegexExtractor.default");
    }

    public void setTemplate(String template) {
        this.setProperty("RegexExtractor.template", template);
    }

    public String getTemplate() {
        return this.getPropertyAsString("RegexExtractor.template");
    }

    public boolean useHeaders() {
        return "true".equalsIgnoreCase(this.getPropertyAsString("RegexExtractor.useHeaders"));
    }

    public boolean useBody() {
        String prop = this.getPropertyAsString("RegexExtractor.useHeaders");
        return prop.length() == 0 || "false".equalsIgnoreCase(prop);
    }

    public boolean useUnescapedBody() {
        String prop = this.getPropertyAsString("RegexExtractor.useHeaders");
        return "unescaped".equalsIgnoreCase(prop);
    }

    public boolean useBodyAsDocument() {
        String prop = this.getPropertyAsString("RegexExtractor.useHeaders");
        return "as_document".equalsIgnoreCase(prop);
    }

    public boolean useUrl() {
        String prop = this.getPropertyAsString("RegexExtractor.useHeaders");
        return "URL".equalsIgnoreCase(prop);
    }

    public boolean useCode() {
        String prop = this.getPropertyAsString("RegexExtractor.useHeaders");
        return "code".equalsIgnoreCase(prop);
    }

    public boolean useMessage() {
        String prop = this.getPropertyAsString("RegexExtractor.useHeaders");
        return "message".equalsIgnoreCase(prop);
    }

    public void setUseField(String actionCommand) {
        this.setProperty("RegexExtractor.useHeaders", actionCommand);
    }
}
