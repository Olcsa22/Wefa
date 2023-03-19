package hu.lanoga.wefa.service;

import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.service.DefaultCrudService;
import hu.lanoga.wefa.model.WefaProcessInstance;
import hu.lanoga.wefa.repository.WefaProcessInstanceJdbcRepository;

@Service
public class WefaProcessInstanceService extends DefaultCrudService<WefaProcessInstance, WefaProcessInstanceJdbcRepository> {

	public WefaProcessInstance findOneByProcessInstanceId(final String processInstanceId) {
		return this.repository.findOneBy("processInstanceId", processInstanceId);
	}

}
