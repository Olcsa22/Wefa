package hu.lanoga.toolbox.chat.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "chatEntryJdbcRepositoryOverrideBean")
@Repository
public class ChatEntryJdbcRepository extends DefaultJdbcRepository<ChatEntry> {
	
	@Override
	public String getInnerSelect() {
		return "SELECT e.*, extr_from_lang(csi1.caption, '#lang1', '#lang2') AS target_type_caption, substring(e.message_text from 0 for 55) AS message_text_ellipsis FROM chat_entry e INNER JOIN code_store_item csi1 ON e.target_type = csi1.id";
	}
	
	public List<ChatEntry> findAllUserTargetType(int targetUserId, int currentUserId, final Integer msgIdAnchor, boolean goUp) {

		final MapSqlParameterSource namedParameters = new MapSqlParameterSource().addValue("targetType", ToolboxSysKeys.ChatTargetType.AUTH_USER)
				.addValue("targetUserId", targetUserId).addValue("currentUserId", currentUserId)
				.addValue("targetUserIdStr", Integer.toString(targetUserId)).addValue("currentUserIdStr", Integer.toString(currentUserId));

		final StringBuilder sbQuery = new StringBuilder();
		sbQuery.append(this.selectQuery);
		sbQuery.append(" AND (target_type = :targetType) AND (");
		sbQuery.append("(target_value = :targetUserIdStr AND created_by = :currentUserId)");
		sbQuery.append("OR");
		sbQuery.append("(target_value = :currentUserIdStr AND created_by = :targetUserId)");
		sbQuery.append(") ");
		
		if (msgIdAnchor != null) {
			namedParameters.addValue("msgIdAnchor", msgIdAnchor);
			if (goUp) {
				sbQuery.append(" AND (id < :msgIdAnchor) ");
			} else {
				sbQuery.append(" AND (id > :msgIdAnchor) ");
			}
		}
		
		sbQuery.append("ORDER BY ");
		sbQuery.append(this.idColumnName);
		sbQuery.append(" DESC");

		if (msgIdAnchor == null || goUp) {
			sbQuery.append(this.jdbcRepositoryManager.buildSqlLimitPart(PageRequest.of(0, 10)));
		}
		
		List<ChatEntry> resultList = this.namedParameterJdbcTemplate.query(this.fillVariables(sbQuery.toString()), namedParameters, this.newRowMapperInstance());

		if (!goUp) {
			
			updateAllToSeenUserTargetType(currentUserId);
			
		}

		return resultList;
		
	}
	
	/**
	 * Olyan userek, akik nemrég küldtek üzenetet (privát chat) a most belépett usernek (és a seen flag még false)
	 * 
	 * @param currentUserId
	 * @return
	 */
	public Set<Integer> findNotSeenChatCreatorsUserTargetType(int currentUserId) {
		
		final MapSqlParameterSource namedParameters = new MapSqlParameterSource().addValue("targetType", ToolboxSysKeys.ChatTargetType.AUTH_USER)
				.addValue("currentUserIdStr", Integer.toString(currentUserId));

		final StringBuilder sbQuery = new StringBuilder();
		sbQuery.append(" select created_by from chat_entry where #tenantCondition");
		sbQuery.append(" AND (target_type = :targetType) AND (target_value = :currentUserIdStr) AND seen = FALSE");
		
		List<Integer> resultList = this.namedParameterJdbcTemplate.queryForList(this.fillVariables(sbQuery.toString()), namedParameters, Integer.class);
		
		return new HashSet<Integer>(resultList);
		
	}
	
	private void updateAllToSeenUserTargetType(int currentUserId) {
		
		final MapSqlParameterSource namedParameters = new MapSqlParameterSource().addValue("targetType", ToolboxSysKeys.ChatTargetType.AUTH_USER).addValue("currentUserIdStr", Integer.toString(currentUserId));
				
		this.namedParameterJdbcTemplate.update(this.fillVariables(this.updateQuery + " seen = true WHERE #tenantCondition AND target_type = :targetType AND target_value = :currentUserIdStr"), namedParameters);
		
	}
	
	public List<ChatEntry> findAllRoleTargetType(String roleName, final Integer msgIdAnchor, boolean goUp) {

		final MapSqlParameterSource namedParameters = new MapSqlParameterSource().addValue("targetType", ToolboxSysKeys.ChatTargetType.AUTH_ROLE).addValue("targetValue", roleName);

		final StringBuilder sbQuery = new StringBuilder();
		sbQuery.append(this.selectQuery);
		sbQuery.append(" AND ");
		sbQuery.append("(target_type = :targetType AND target_value = :targetValue)");
		
		if (msgIdAnchor != null) {
			namedParameters.addValue("msgIdAnchor", msgIdAnchor);
			if (goUp) {
				sbQuery.append(" AND (id < :msgIdAnchor) ");
			} else {
				sbQuery.append(" AND (id > :msgIdAnchor) ");
			}
		}
		
		sbQuery.append(" ORDER BY ");
		sbQuery.append(this.idColumnName);
		sbQuery.append(" DESC");

		if (msgIdAnchor == null || goUp) {
			sbQuery.append(this.jdbcRepositoryManager.buildSqlLimitPart(PageRequest.of(0, 10)));
		}
		
		return this.namedParameterJdbcTemplate.query(this.fillVariables(sbQuery.toString()), namedParameters, this.newRowMapperInstance());
		
	}

}
