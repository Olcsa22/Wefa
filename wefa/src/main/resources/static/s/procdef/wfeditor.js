var modeler, elementRegistry;

function buildWfEditor(connector) {

    var activityDescriptor = {
            "name": "Activiti",
            "prefix": "activiti",
            "uri": "http://www.activity.org/bpmn",
            "xml": {
                "tagAlias": "lowerCase"
            },
            "associations": [],
            "types": [
                {
                    "name": "ExpandedTasks",
                    "extends": [
                        "bpmn:FlowNode"
                    ],
                    "properties": [
                        {
                            "name": "formKey",
                            "isAttr": true,
                            "type": "String"
                        },
                        {
                            "name": "assignee",
                            "isAttr": true,
                            "type": "String"
                        }
                    ]
                },
            ]
        },
        activityPaletteModule = {
            __init__: ['activityPaletteProvider'],
            activityPaletteProvider: ['type', ActivityPaletteProvider]
        };

    var processDefinitionId = connector.getState().processDefinitionId;
    var areaId = connector.getState().areaId;

    console.log(processDefinitionId);
    console.log(areaId);

    function ActivityPaletteProvider(palette, create, elementFactory) {
        this._create = create;
        this._elementFactory = elementFactory;

        palette.registerProvider(this);
    }

    ActivityPaletteProvider.prototype.getPaletteEntries = function () {

        var elementFactory = this._elementFactory,
            create = this._create;

        function startCreateUserTask(event) {
            var serviceTaskShape = elementFactory.create('shape', {type: 'bpmn:UserTask'});
            create.start(event, serviceTaskShape);
        }

        function startCreateScriptTask(event) {
            var serviceTaskShape = elementFactory.create('shape', {type: 'bpmn:ScriptTask'});
            create.start(event, serviceTaskShape);
        }

        return {
            'create-user-task': {
                group: 'activity',
                title: 'Create UserTask',
                className: 'bpmn-icon-user-task',
                action: {
                    dragstart: startCreateUserTask,
                    click: startCreateUserTask
                }
            },
            'create-script-task': {
                group: 'activity',
                title: 'Create ScriptTask',
                className: 'bpmn-icon-script-task',
                action: {
                    dragstart: startCreateScriptTask,
                    click: startCreateScriptTask
                }
            },
        };
    };

    $('head').append('<link rel="stylesheet" type="text/css" href="s/procdef/bower_components/bpmn-js/dist/assets/diagram-js.css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="s/procdef/bower_components/bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css">');

    var wrapper = $("#" + areaId + ">.v-expand");
    var BpmnModeler = window.BpmnJS;

    modeler = new BpmnModeler({
        container: wrapper,
        moddleExtensions: {
            activiti: activityDescriptor
        },
        additionalModules: [
            activityPaletteModule
        ]
    });
    wrapper.addClass("bjs-container-wrapper");

    var eventBus = modeler.get('eventBus');

    // you may hook into any of the following events
    var events = [
        // 'element.hover',
        // 'element.out',
        // 'element.click',
        'element.dblclick',
        // 'element.mousedown',
        // 'element.mouseup'
    ];

    events.forEach(function (event) {

        eventBus.on(event, function (e) {
        	
            var type = e.element.type,
                businessObject = e.element.businessObject;
            
            // console.log(e.element.type, processDefinitionId, e.element.id);

            if (type == 'bpmn:UserTask') {
                wfeUserTaskNodeClick(processDefinitionId, e.element.id, businessObject.assignee, businessObject.formKey);
            }

            if (type == 'bpmn:ScriptTask') {

                if (businessObject.script) {
                    wfeScriptTaskNodeClick(processDefinitionId, e.element.id, businessObject.script);
                } else {
                    wfeScriptTaskNodeClick(processDefinitionId, e.element.id, null);
                }

            }

            if (type == 'bpmn:SequenceFlow') {

                if (businessObject.sourceRef.$type.indexOf("Gateway") === -1) {
                    return;
                }
                if (businessObject.conditionExpression) {
                    wfeConditionalFlowClick(processDefinitionId, e.element.id, businessObject.conditionExpression.body);
                } else {
                    wfeConditionalFlowClick(processDefinitionId, e.element.id, null);
                }

            }
        });
    });

    wfeLoadOrig(processDefinitionId);

}

function wfeLoadOrigBack(processDefinitionId, xml) {

    xml = decodeURIComponent(xml);

    console.log('wfeLoadOrigBack');
    //console.log(processDefinitionId);
    //console.log(xml); // új wf-nél az üres template-et adja vissza (benne egy start elemmel), ha null, akkor az már hiba (lásd szerver log)

    modeler.importXML(xml, function (err) {

        if (!err) {
            modeler.get('canvas').zoom('fit-viewport');
            console.log('success!');
        } else {
            console.log('something went wrong:', err);
        }
    });

}

function wfePreSaveWorkflowBack(processDefinitionId, procDefName) {

    procDefName = decodeURIComponent(procDefName);

    console.log('wfePreSaveWorkflowBack');
    console.log(processDefinitionId);
    console.log(procDefName); // ezt kell beírni az <process id="demo-4" name="demo-4" isExecutable="true">... name részébe

    var elementRegistry = modeler.get('elementRegistry'),
        businessObject = elementRegistry.getAll()[0].businessObject;

    businessObject.name = procDefName;

    modeler.saveXML({format: true}, function (err, xml) {
        
    	if (err) {
            console.error(err);
        } else {
            wfeSaveWorkflowClick(procDefName, btoa(xml));  
        }
    	
    });
}

function wfeSaveWorkflowBack(processDefinitionId, isSuccess) {

    // itt nincs semmi érdemi dolog, amit vissza kell adni, csak debug célra

    console.log('wfeSaveWorkflowBack');
    console.log(processDefinitionId);
    console.log(isSuccess); // nem kell hibaüzenet (Vaadin-ból lesz majd), csak debug célra
}

function wfeConditionalFlowBack(processDefinitionId, elementId, condExpr) {

    // példa: <conditionExpression xsi:type="tFormalExpression"><![CDATA[${documentsApproved== 'false'}]]></conditionExpression>
    // tehát condExpr-ben stringként ez: <![CDATA[${documentsApproved== 'false'}]]>

    condExpr = decodeURIComponent(condExpr);

    console.log('wfeConditionalFlowBack');
    console.log(processDefinitionId);
    console.log(elementId);
    console.log(condExpr); // ez base64!

    var moddle = modeler.get('moddle');
    var newCondition = moddle.create('bpmn:FormalExpression', {
        body: condExpr
    });

    var elementRegistry = modeler.get('elementRegistry'),
        businessObject = elementRegistry.get(elementId).businessObject;

    businessObject.conditionExpression = newCondition;
}

function wfeUserTaskNodeBack(processDefinitionId, elementId, activitiAssignee, activitiFormKey) {

    // példa: <userTask id="documentCheckTask" name="Jóváhagyás" activiti:assignee="dr.kiss.jozsef" activiti:formKey="documentCheckForm" activiti:priority="1">
    // tehát activitiAssignee-ben String-ként dr.kiss.jozsef stb.

    console.log('wfeUserTaskNodeBack');
    console.log(processDefinitionId);
    console.log(elementId);
    console.log(activitiAssignee);
    console.log(activitiFormKey);

    // https://github.com/bpmn-io/bpmn-js-examples/blob/bccefe24eab5f7ba1045f549086dba4ca7463375/bpmn-properties/README.md

    var elementRegistry = modeler.get('elementRegistry'),
        businessObject = elementRegistry.get(elementId).businessObject;

    businessObject.formKey = activitiFormKey;
    businessObject.assignee = activitiAssignee;
}

function wfeScriptTaskNodeBack(processDefinitionId, elementId, scriptStr) {

    // példa:
    // <scriptTask id="sendTask" name="Send" scriptFormat="groovy">
    // <script><![CDATA[System.out.println("----------- WF email sender (" + execution.getVariable("clientName") + ") -----------");
    // hu.lanoga.actwf.util.DemoEmailUtil.sendMail(execution.getVariable("clientEmail"), execution.getVariable("clientName")); ]]></script>
    // </scriptTask>
    // tehát a scriptStr-ben String-ként <![CDATA[System.out.print...

    // scriptStr = decodeURIComponent(scriptStr);

    console.log('wfeScriptTaskNodeBack');
    console.log(processDefinitionId);
    console.log(elementId);
    console.log(scriptStr);

    var elementRegistry = modeler.get('elementRegistry'),
        businessObject = elementRegistry.get(elementId).businessObject;

    businessObject.script = atob(scriptStr); // úgy tűnik itt sem kell idetenni a ![CDATA[...-t
    businessObject.scriptFormat = 'groovy';
}

hu_lanoga_wefa_vaadin_procdef_ProcessDefinitionEditorComponent = function () {

    this.onStateChange = function () {

        buildWfEditor(this);

    }

}
