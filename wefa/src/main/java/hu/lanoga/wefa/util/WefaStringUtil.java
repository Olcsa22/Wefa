package hu.lanoga.wefa.util;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.springframework.http.MediaType;

import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.wefa.SysKeys;

public class WefaStringUtil {
	
	private WefaStringUtil() {
		//
	}

    /**
     * Visszaadja összekonkatenálva paraméterül kapott org.springframework.http.MediaType-okat. 
     * Ha üres/null, akkor "application/pdf" lesz a default érték
     *
     * @param mediaTypes 
     * 		org.springframework.http.MediaType
     * @return
     */
    public static String getMimeTypeAsString(MediaType... mediaTypes) {
        StringBuilder sb = new StringBuilder();
        String types;

        if (mediaTypes.length == 0) {
            types = "application/pdf";
        } else {
            for (MediaType m : mediaTypes) {
                sb.append(m.toString());
                sb.append(",");
            }
            types = sb.toString();
            types = types.substring(0, types.length() - 1);
        }
        return types;
    }

    public static List<String> getUserRoleAssigneeTags(final ToolboxUserDetails userDetails) {

        List<String> tags = new ArrayList<>();

        JSONArray ja = new JSONArray(userDetails.getUserRoles());
        
        for (int i = 0; i < ja.length(); ++i) {
            tags.add(getUserRoleTagByRoleId(ja.getInt(i)));
        }

        return tags;
    }

    public static String getUserRoleTagByRoleId(final int roleId) {
        return SysKeys.USER_ROLE_TAG + roleId;
    }

    public static String getRoleIdByUserRoleTag(final String tag) {
        return tag.replace(SysKeys.USER_ROLE_TAG, "");
    }

}
