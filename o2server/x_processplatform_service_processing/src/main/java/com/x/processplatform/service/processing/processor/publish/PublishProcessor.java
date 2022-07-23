package com.x.processplatform.service.processing.processor.publish;

import com.google.gson.JsonElement;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.Applications;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.jaxrs.WoId;
import com.x.base.core.project.jaxrs.WrapBoolean;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.base.core.project.scripting.JsonScriptingExecutor;
import com.x.base.core.project.scripting.ScriptingFactory;
import com.x.base.core.project.tools.ListTools;
import com.x.base.core.project.tools.StringTools;
import com.x.base.core.project.x_cms_assemble_control;
import com.x.base.core.project.x_query_service_processing;
import com.x.processplatform.core.entity.content.Attachment;
import com.x.processplatform.core.entity.content.Data;
import com.x.processplatform.core.entity.content.Review;
import com.x.processplatform.core.entity.content.Work;
import com.x.processplatform.core.entity.element.Activity;
import com.x.processplatform.core.entity.element.Publish;
import com.x.processplatform.core.entity.element.PublishTable;
import com.x.processplatform.core.entity.element.Route;
import com.x.processplatform.core.entity.log.Signal;
import com.x.processplatform.service.processing.Business;
import com.x.processplatform.service.processing.ThisApplication;
import com.x.processplatform.service.processing.WrapScriptObject;
import com.x.processplatform.service.processing.processor.AeiObjects;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据发布节点处理器
 * @author sword
 */
public class PublishProcessor extends AbstractPublishProcessor {

	public static final Logger LOGGER = LoggerFactory.getLogger(PublishProcessor.class);
	private static final String CMS_PUBLISH_URI = "document/cipher/publish/content";

	public PublishProcessor(EntityManagerContainer entityManagerContainer) throws Exception {
		super(entityManagerContainer);
	}

	@Override
	protected Work arriving(AeiObjects aeiObjects, Publish publish) throws Exception {
		// 发送ProcessingSignal
		aeiObjects.getProcessingAttributes()
				.push(Signal.publishArrive(aeiObjects.getWork().getActivityToken(), publish));
		return aeiObjects.getWork();
	}

	@Override
	protected void arrivingCommitted(AeiObjects aeiObjects, Publish publish) throws Exception {
		// Do nothing
	}

	@Override
	protected List<Work> executing(AeiObjects aeiObjects, Publish publish) throws Exception {
		// 发送ProcessingSignal
		aeiObjects.getProcessingAttributes()
				.push(Signal.publishExecute(aeiObjects.getWork().getActivityToken(), publish));
		List<Work> results = new ArrayList<>();
		boolean passThrough = false;
		switch (publish.getPublishTarget()) {
			case Publish.PUBLISH_TARGET_CMS:
				// 发布到内容管理
				passThrough = this.publishToCms(aeiObjects, publish);
				break;
			case Publish.PUBLISH_TARGET_TABLE:
				// 发布到数据中心自建表
				passThrough = this.publishToTable(aeiObjects, publish);
				break;
			default:
				break;
		}
		if (passThrough) {
			results.add(aeiObjects.getWork());
		} else {
			LOGGER.info("work title:{}, id:{} public return false, stay in the current activity.",
					() -> aeiObjects.getWork().getTitle(), () -> aeiObjects.getWork().getId());
		}
		return results;
	}

	private boolean publishToTable(AeiObjects aeiObjects, Publish publish) throws Exception {
		List<AssignPublish> list = this.evalTableBody(aeiObjects, publish);
		boolean flag = true;
		for (AssignPublish assignPublish : list){
			WrapBoolean resp = ThisApplication.context().applications().postQuery(x_query_service_processing.class,
					Applications.joinQueryUri("table", assignPublish.getTableName(), "update", aeiObjects.getWork().getJob()), assignPublish.getData())
					.getData(WrapBoolean.class);
			LOGGER.debug("publish to table：{}, result：{}",assignPublish.getTableName(),resp.getValue());
			if(BooleanUtils.isFalse(resp.getValue())){
				flag = false;
			}
		}
		return flag;
	}

	private List<AssignPublish> evalTableBody(AeiObjects aeiObjects, Publish publish) throws Exception {
		List<AssignPublish> list = new ArrayList<>();
		if(ListTools.isNotEmpty(publish.getPublishTableList())){
			for (PublishTable publishTable : publish.getPublishTableList()){
				AssignPublish assignPublish = new AssignPublish();
				assignPublish.setTableName(publishTable.getTableName());
				if(PublishTable.TABLE_DATA_BY_PATH.equals(publishTable.getQueryTableDataBy())){
					if(StringUtils.isNotBlank(publishTable.getQueryTableDataPath())){
						Object o = aeiObjects.getData().find(publishTable.getQueryTableDataPath());
						if(o!=null){
							assignPublish.setData(gson.toJsonTree(o));
						}
					}
				}else {
					WrapScriptObject assignBody = new WrapScriptObject();
					if (hasTableAssignDataScript(publishTable)) {
						ScriptContext scriptContext = aeiObjects.scriptContext();
						CompiledScript cs = aeiObjects.business().element().getCompiledScript(aeiObjects.getApplication().getId(),
								publishTable.getTargetAssignDataScript(), publishTable.getTargetAssignDataScriptText());
						scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put(ScriptingFactory.BINDING_NAME_JAXRSBODY,
								assignBody);
						JsonScriptingExecutor.jsonElement(cs, scriptContext, o -> {
							if (!o.isJsonNull()) {
								assignPublish.setData(o);
							}
						});
					}
				}
				if(assignPublish.getData() == null){
					assignPublish.setData(gson.toJsonTree(aeiObjects.getData()));
				}
				list.add(assignPublish);
			}
		}
		return list;
	}

	private boolean publishToCms(AeiObjects aeiObjects, Publish publish) throws Exception {
		CmsDocument cmsDocument = this.evalCmsBody(aeiObjects, publish);
		cmsDocument.setWf_workId(aeiObjects.getWork().getId());
		cmsDocument.setWf_jobId(aeiObjects.getWork().getJob());
		String categoryId = "";
		if(Publish.CMS_CATEGORY_FROM_DATA.equals(publish.getCategorySelectType())){
			if(StringUtils.isNotBlank(publish.getCategoryIdDataPath())) {
				categoryId = (String) aeiObjects.getData().find(publish.getCategoryIdDataPath());
			}
		}else{
			categoryId = publish.getCategoryId();
		}
		if(StringUtils.isBlank(categoryId)){
			LOGGER.warn("流程数据发布到内容管理失败：分类ID为空！");
			return false;
		}
		cmsDocument.setCategoryId(categoryId);
		if(BooleanUtils.isTrue(publish.getUseProcessForm())){
			cmsDocument.setWf_formId(aeiObjects.getWork().getForm());
		}
		if(BooleanUtils.isTrue(publish.getInheritAttachment())){
			List<Attachment> attachments = aeiObjects.getAttachments();
			if(ListTools.isNotEmpty(attachments)){
				cmsDocument.setWf_attachmentIds(ListTools.extractField(attachments, JpaObject.id_FIELDNAME, String.class, true, true));
			}
		}
		String title = "";
		if(StringUtils.isNotBlank(publish.getTitleDataPath())){
			title = (String)aeiObjects.getData().find(publish.getTitleDataPath());
		}
		if(StringUtils.isBlank(title)){
			title = aeiObjects.getWork().getTitle();
		}
		if(StringUtils.isBlank(title)){
			title = "未命名";
		}
		cmsDocument.setTitle(StringTools.utf8SubString(title, 255));
		List<CmsPermission> readerList = new ArrayList<>();
		List<String> list = this.findPathData(aeiObjects.getData(), publish.getReaderDataPathList());
		if(ListTools.isNotEmpty(list)) {
			List<Review> reviewList = aeiObjects.getReviews();
			if(ListTools.isNotEmpty(reviewList)){
				list.addAll(ListTools.extractField(reviewList, Review.person_FIELDNAME, String.class, true, true));
			}
		}

		list.stream().forEach(s -> {
			CmsPermission cmsPermission = new CmsPermission();
			cmsPermission.setPermissionObjectName(s);
			readerList.add(cmsPermission);
		});
		cmsDocument.setReaderList(readerList);
		List<CmsPermission> authorList = new ArrayList<>();
		list = this.findPathData(aeiObjects.getData(), publish.getAuthorDataPathList());
		list.add(aeiObjects.getWork().getCreatorPerson());
		list.stream().forEach(s -> {
			CmsPermission cmsPermission = new CmsPermission();
			cmsPermission.setPermissionObjectName(s);
			authorList.add(cmsPermission);
		});
		cmsDocument.setAuthorList(authorList);
		cmsDocument.setPictureList(this.findPathData(aeiObjects.getData(), publish.getPictureDataPathList()));
		WoId woId = ThisApplication.context().applications().putQuery(x_cms_assemble_control.class, CMS_PUBLISH_URI, cmsDocument).getData(WoId.class);
		if(woId==null || StringUtils.isBlank(woId.getId())){
			return false;
		}
		//发送消息通知
		if(StringUtils.isNotBlank(publish.getNotifyDataPathList())) {
			list = this.findPathData(aeiObjects.getData(), publish.getNotifyDataPathList());
			if (ListTools.isNotEmpty(list)) {
				CmsNotify cmsNotify = new CmsNotify(list);
				ThisApplication.context().applications().postQuery(x_cms_assemble_control.class,
						Applications.joinQueryUri("document", woId.getId(), "update", aeiObjects.getWork().getJob()), cmsNotify);
			}
		}
		return true;
	}

	private CmsDocument evalCmsBody(AeiObjects aeiObjects, Publish publish) throws Exception {
		CmsDocument cmsDocument = new CmsDocument();
		WrapScriptObject assignBody = new WrapScriptObject();
		if (hasCmsAssignDataScript(publish)) {
			ScriptContext scriptContext = aeiObjects.scriptContext();
			CompiledScript cs = aeiObjects.business().element().getCompiledScript(aeiObjects.getApplication().getId(),
					aeiObjects.getActivity(), Business.EVENT_PUBLISHCMSBODY);
			scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put(ScriptingFactory.BINDING_NAME_JAXRSBODY,
					assignBody);
			JsonScriptingExecutor.jsonElement(cs, scriptContext, o -> {
				if (!o.isJsonNull()) {
					cmsDocument.setData(o);
				}
			});
		}
		if(cmsDocument.getData() == null){
			cmsDocument.setData(gson.toJsonTree(aeiObjects.getData()));
		}

		return cmsDocument;
	}

	/** 取得通过路径指定的人员组织 */
	private List<String> findPathData(Data data, String paths) throws Exception {
		List<String> list = new ArrayList<>();
		if (StringUtils.isNotBlank(paths)) {
			for (String str : paths.split(",")) {
				if (StringUtils.isNotBlank(str)) {
					list.addAll(data.extractDistinguishedName(str.trim()));
				}
			}
		}
		return list;
	}

	public class AssignPublish {

		private String tableName;

		private JsonElement data;

		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public JsonElement getData() {
			return data;
		}

		public void setData(JsonElement data) {
			this.data = data;
		}
	}

	public class CmsDocument extends GsonPropertyObject{
		private String title;
		private String wf_workId;
		private String wf_jobId;
		private String wf_formId;
		private String summary;
		private String categoryId;
		private List<String> pictureList;
		private List<String> wf_attachmentIds;
		private JsonElement data;
		private List<CmsPermission> readerList;
		private List<CmsPermission> authorList;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getWf_workId() {
			return wf_workId;
		}

		public void setWf_workId(String wf_workId) {
			this.wf_workId = wf_workId;
		}

		public String getWf_jobId() {
			return wf_jobId;
		}

		public void setWf_jobId(String wf_jobId) {
			this.wf_jobId = wf_jobId;
		}

		public String getWf_formId() {
			return wf_formId;
		}

		public void setWf_formId(String wf_formId) {
			this.wf_formId = wf_formId;
		}

		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		public List<String> getPictureList() {
			return pictureList;
		}

		public void setPictureList(List<String> pictureList) {
			this.pictureList = pictureList;
		}

		public List<String> getWf_attachmentIds() {
			return wf_attachmentIds;
		}

		public void setWf_attachmentIds(List<String> wf_attachmentIds) {
			this.wf_attachmentIds = wf_attachmentIds;
		}

		public JsonElement getData() {
			return data;
		}

		public void setData(JsonElement data) {
			this.data = data;
		}

		public List<CmsPermission> getReaderList() {
			return readerList;
		}

		public void setReaderList(List<CmsPermission> readerList) {
			this.readerList = readerList;
		}

		public List<CmsPermission> getAuthorList() {
			return authorList;
		}

		public void setAuthorList(List<CmsPermission> authorList) {
			this.authorList = authorList;
		}

		public String getCategoryId() {
			return categoryId;
		}

		public void setCategoryId(String categoryId) {
			this.categoryId = categoryId;
		}
	}

	public class CmsPermission {
		private String permissionObjectName;

		public String getPermissionObjectName() {
			return permissionObjectName;
		}

		public void setPermissionObjectName(String permissionObjectName) {
			this.permissionObjectName = permissionObjectName;
		}
	}

	public class CmsNotify extends GsonPropertyObject{

		public CmsNotify(List<String> notifyPersonList){
			this.notifyPersonList = notifyPersonList;
		}
		private List<String> notifyPersonList;
		private Boolean notifyByDocumentReadPerson = false;
		private Boolean notifyCreatePerson = false;

		public List<String> getNotifyPersonList() {
			return notifyPersonList;
		}

		public void setNotifyPersonList(List<String> notifyPersonList) {
			this.notifyPersonList = notifyPersonList;
		}

		public Boolean getNotifyByDocumentReadPerson() {
			return notifyByDocumentReadPerson;
		}

		public void setNotifyByDocumentReadPerson(Boolean notifyByDocumentReadPerson) {
			this.notifyByDocumentReadPerson = notifyByDocumentReadPerson;
		}

		public Boolean getNotifyCreatePerson() {
			return notifyCreatePerson;
		}

		public void setNotifyCreatePerson(Boolean notifyCreatePerson) {
			this.notifyCreatePerson = notifyCreatePerson;
		}
	}

	@Override
	protected void executingCommitted(AeiObjects aeiObjects, Publish publish, List<Work> works) throws Exception {
		// Do nothing
	}

	@Override
	protected List<Route> inquiring(AeiObjects aeiObjects, Publish publish) throws Exception {
		// 发送ProcessingSignal
		aeiObjects.getProcessingAttributes()
				.push(Signal.publishInquire(aeiObjects.getWork().getActivityToken(), publish));
		List<Route> results = new ArrayList<>();
		results.add(aeiObjects.getRoutes().get(0));
		return results;
	}

	@Override
	protected void inquiringCommitted(AeiObjects aeiObjects, Publish publish) throws Exception {
		// Do nothing
	}

	private boolean hasCmsAssignDataScript(Publish publish) {
		return StringUtils.isNotEmpty(publish.getTargetAssignDataScript())
				|| StringUtils.isNotEmpty(publish.getTargetAssignDataScriptText());
	}

	private boolean hasTableAssignDataScript(PublishTable publishTable) {
		return StringUtils.isNotEmpty(publishTable.getTargetAssignDataScript())
				|| StringUtils.isNotEmpty(publishTable.getTargetAssignDataScriptText());
	}
}
