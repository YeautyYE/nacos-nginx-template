package org.yeauty.service.impl;

import org.yeauty.service.StartupService;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartupServiceImpl implements StartupService {

    @Override
    public Map<String, String> argsToMap(String[] args) {
        HashMap<String, String> map = new HashMap<>();
        String reg = "^--[^=]+=.+";
        Pattern pattern = Pattern.compile(reg);
        for (String arg : args) {
            Matcher matcher = pattern.matcher(arg);
            if (!matcher.matches()) {
                continue;
            }
            String group = matcher.group();
            int i = group.indexOf("=");
            if (i == group.length() - 1) {
                continue;
            }
            map.put(group.substring(2, i), group.substring(i + 1));
        }
        return map;
    }
}
