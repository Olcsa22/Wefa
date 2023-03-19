package hu.lanoga.toolbox.chat.internal;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.service.DefaultCrudService;
import hu.lanoga.toolbox.service.LazyEnhanceCrudService;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.UserJdbcRepository;

@Service
public class ChatEntryService extends DefaultCrudService<ChatEntry, ChatEntryJdbcRepository> implements LazyEnhanceCrudService<ChatEntry> {

	@Autowired
	private FileStoreService fileStoreService;
	
	@Autowired
	private UserJdbcRepository userJdbcRepository;

	@Override
	public void delete(final int id) {

		final ChatEntry t = this.repository.findOne(id);

		SecurityUtil.limitAccessAdminOrOwner(t.getCreatedBy());

		this.fileStoreService.setToBeDeleted(t.getFileIds());

		super.delete(id);
	}

	@Override
	public ChatEntry save(final ChatEntry t) {

		if (!t.isNew()) {
			SecurityUtil.limitAccessAdminOrOwner(t.getCreatedBy());
		}

		this.fileStoreService.setToNormal(t.getFileIds());

		return super.save(t);
	}

	@Override
	public ChatEntry enhance(ChatEntry t) {

		t.setMessageTextEllipsis(StringUtils.abbreviate(t.getMessageTextEllipsis(), 50));
		
		if (t.getCreatedBy() != null) {
			t.setCreatedByCaption(I18nUtil.buildFullName(this.userJdbcRepository.findOne(t.getCreatedBy()), true, false));
		}
		
		return null;
	}
	

	
	public List<ChatEntry> findAllUserTargetType(int targetUserId, final Integer msgIdAnchor, boolean goUp) {
				
		return this.repository.findAllUserTargetType(targetUserId, SecurityUtil.getLoggedInUser().getId(), msgIdAnchor, goUp);
		
	}
	
	/**
	 * Olyan userek, akik nemrég küldtek üzenetet (privát chat) a most belépett usernek (és a seen flag még false)
	 * 
	 * @param currentUserId
	 * @return
	 */
	public Set<Integer> findNotSeenChatCreatorsUserTargetType() {
				
		return this.repository.findNotSeenChatCreatorsUserTargetType(SecurityUtil.getLoggedInUser().getId());
		
	}
	
	public List<ChatEntry> findAllRoleTargetType(String roleName, final Integer msgIdAnchor, boolean goUp) {
	
		SecurityUtil.limitAccessHasAnyRole(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR, roleName);
		
		return this.repository.findAllRoleTargetType(roleName, msgIdAnchor, goUp);
		
	}
	
	
}