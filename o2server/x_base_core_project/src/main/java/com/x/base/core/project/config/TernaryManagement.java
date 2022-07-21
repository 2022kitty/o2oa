package com.x.base.core.project.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.gson.XGsonBuilder;
import com.x.base.core.project.http.TokenType;
import com.x.base.core.project.organization.OrganizationDefinition;
import com.x.base.core.project.tools.BaseTools;
import com.x.base.core.project.tools.Crypto;
import com.x.base.core.project.tools.DefaultCharset;

/**
 * 三元管理配置
 * 
 * @author sword
 */
public class TernaryManagement extends ConfigObject {

	public static final String initPassword = "o2";

	public static final String INIT_SYSTEM_MANAGER = "systemManager";
	public static final String INIT_SYSTEM_MANAGER_DISTINGUISHED_NAME = "系统管理员@systemManager@P";
	public static final String INIT_SYSTEM_MANAGER_NAME = "系统管理员";

	public static final String INIT_SECURITY_MANAGER = "securityManager";
	public static final String INIT_SECURITY_MANAGER_DISTINGUISHED_NAME = "安全管理员@securityManager@P";
	public static final String INIT_SECURITY_MANAGER_NAME = "安全管理员";

	public static final String INIT_AUDIT_MANAGER = "auditManager";
	public static final String INIT_AUDIT_MANAGER_DISTINGUISHED_NAME = "安全审计员@auditManager@P";
	public static final String INIT_AUDIT_MANAGER_NAME = "安全审计员";

	private transient String _systemManagerPassword;

	private transient String _securityManagerPassword;

	private transient String _auditManagerPassword;

	public static TernaryManagement defaultInstance() {
		TernaryManagement o = new TernaryManagement();
		return o;
	}

	public TernaryManagement() {
		this.enable = false;
		this.systemManagerPassword = "";
		this.securityManagerPassword = "";
		this.auditManagerPassword = "";
	}

	public boolean isTernaryManagement(String name) {
		return StringUtils.equals(INIT_SYSTEM_MANAGER, name)
				|| StringUtils.equals(INIT_SYSTEM_MANAGER_DISTINGUISHED_NAME, name)
				|| StringUtils.equals(INIT_SYSTEM_MANAGER_NAME, name) || StringUtils.equals(INIT_SECURITY_MANAGER, name)
				|| StringUtils.equals(INIT_SECURITY_MANAGER_DISTINGUISHED_NAME, name)
				|| StringUtils.equals(INIT_SECURITY_MANAGER_NAME, name) || StringUtils.equals(INIT_AUDIT_MANAGER, name)
				|| StringUtils.equals(INIT_AUDIT_MANAGER_DISTINGUISHED_NAME, name)
				|| StringUtils.equals(INIT_AUDIT_MANAGER_NAME, name);
	}

	public boolean isSystemManager(String name) {
		return StringUtils.equals(INIT_SYSTEM_MANAGER, name)
				|| StringUtils.equals(INIT_SYSTEM_MANAGER_DISTINGUISHED_NAME, name)
				|| StringUtils.equals(INIT_SYSTEM_MANAGER_NAME, name);
	}

	public boolean isSecurityManager(String name) {
		return StringUtils.equals(INIT_SECURITY_MANAGER, name)
				|| StringUtils.equals(INIT_SECURITY_MANAGER_DISTINGUISHED_NAME, name)
				|| StringUtils.equals(INIT_SECURITY_MANAGER_NAME, name);
	}

	public boolean isAuditManager(String name) {
		return StringUtils.equals(INIT_AUDIT_MANAGER, name)
				|| StringUtils.equals(INIT_AUDIT_MANAGER_DISTINGUISHED_NAME, name)
				|| StringUtils.equals(INIT_AUDIT_MANAGER_NAME, name);
	}

	public boolean verifyPassword(String name, String password) {
		if (this.isSystemManager(name)) {
			return StringUtils.equals(this.getSystemManagerPassword(), password);
		} else if (this.isSecurityManager(name)) {
			return StringUtils.equals(this.getSecurityManagerPassword(), password);
		} else if (this.isAuditManager(name)) {
			return StringUtils.equals(this.getAuditManagerPassword(), password);
		} else {
			return false;
		}
	}

	public TokenType getTokenType(String name) {
		if (this.isSystemManager(name)) {
			return TokenType.systemManager;
		} else if (this.isSecurityManager(name)) {
			return TokenType.securityManager;
		} else if (this.isAuditManager(name)) {
			return TokenType.auditManager;
		} else {
			return null;
		}
	}

	public void save() throws Exception {
		File file = new File(Config.base(), Config.PATH_CONFIG_TERNARY_MANAGEMENT);
		FileUtils.write(file, XGsonBuilder.toJson(this), DefaultCharset.charset);
		BaseTools.executeSyncFile(Config.PATH_CONFIG_TERNARY_MANAGEMENT);
	}

	public InitialManager initialManagerInstance(String name) {
		InitialManager o = new InitialManager();
		String distinguishedName = "";
		String userName = name;
		if (isSystemManager(name)) {
			name = INIT_SYSTEM_MANAGER;
			distinguishedName = INIT_SYSTEM_MANAGER_DISTINGUISHED_NAME;
			userName = INIT_SYSTEM_MANAGER_NAME;
		} else if (isSecurityManager(name)) {
			name = INIT_SECURITY_MANAGER;
			distinguishedName = INIT_SECURITY_MANAGER_DISTINGUISHED_NAME;
			userName = INIT_SECURITY_MANAGER_NAME;
		} else if (isAuditManager(name)) {
			name = INIT_AUDIT_MANAGER;
			distinguishedName = INIT_AUDIT_MANAGER_DISTINGUISHED_NAME;
			userName = INIT_AUDIT_MANAGER_NAME;
		}

		o.name = userName;
		o.id = name;
		o.unique = name;
		o.employee = name;
		o.display = name;
		o.mail = name + "@o2oa.net";
		o.setDistinguishedName(distinguishedName);
		o.weixin = "";
		o.qq = "";
		o.weibo = "";
		o.mobile = "";
		o.roleList = new ArrayList<>();
		if (isSystemManager(name)) {
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.SystemManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.ProcessPlatformManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.MeetingManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.QueryManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.CMSManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.ServiceManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.FileManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.PortalManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.AttendanceManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.BBSManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.TeamWorkManager));
		} else if (isSecurityManager(name)) {
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.SecurityManager));
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.GroupManager));
		} else if (isAuditManager(name)) {
			o.roleList.add(OrganizationDefinition.toDistinguishedName(OrganizationDefinition.AuditManager));
		}
		return o;
	}

	public class InitialManager extends GsonPropertyObject {

		private static final long serialVersionUID = 1585586759890226859L;

		private String name;
		private String unique;
		private String id;
		private String distinguishedName;
		private String employee;
		private String display;
		private String mail;
		private String weixin;
		private String qq;
		private String weibo;
		private String mobile;
		private String pinyin;
		private String pinyinInitial;
		private List<String> roleList;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmployee() {
			return employee;
		}

		public void setEmployee(String employee) {
			this.employee = employee;
		}

		public String getDisplay() {
			return display;
		}

		public void setDisplay(String display) {
			this.display = display;
		}

		public String getMail() {
			return mail;
		}

		public void setMail(String mail) {
			this.mail = mail;
		}

		public String getWeixin() {
			return weixin;
		}

		public void setWeixin(String weixin) {
			this.weixin = weixin;
		}

		public String getQq() {
			return qq;
		}

		public void setQq(String qq) {
			this.qq = qq;
		}

		public String getWeibo() {
			return weibo;
		}

		public void setWeibo(String weibo) {
			this.weibo = weibo;
		}

		public String getMobile() {
			return mobile;
		}

		public void setMobile(String mobile) {
			this.mobile = mobile;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public List<String> getRoleList() {
			return roleList;
		}

		public void setRoleList(List<String> roleList) {
			this.roleList = roleList;
		}

		public String getUnique() {
			return unique;
		}

		public void setUnique(String unique) {
			this.unique = unique;
		}

		public String getDistinguishedName() {
			return distinguishedName;
		}

		public void setDistinguishedName(String distinguishedName) {
			this.distinguishedName = distinguishedName;
		}

		public String getPinyin() {
			return pinyin;
		}

		public void setPinyin(String pinyin) {
			this.pinyin = pinyin;
		}

		public String getPinyinInitial() {
			return pinyinInitial;
		}

		public void setPinyinInitial(String pinyinInitial) {
			this.pinyinInitial = pinyinInitial;
		}

	}

	@FieldDescribe("是否启用三元管理.")
	private Boolean enable;

	@FieldDescribe("系统管理员账号密码.")
	private String systemManagerPassword;

	@FieldDescribe("安全管理员账号密码.")
	private String securityManagerPassword;

	@FieldDescribe("安全审计员账号密码.")
	private String auditManagerPassword;

	public Boolean getEnable() {
		return enable;
	}

	public void setEnable(Boolean enable) {
		this.enable = enable;
	}

	public String getSystemManagerPassword() {
		if (StringUtils.isEmpty(this._systemManagerPassword)) {
			String password = StringUtils.isBlank(this.systemManagerPassword) ? initPassword
					: this.systemManagerPassword;
			this._systemManagerPassword = Crypto.plainText(password);
		}
		return this._systemManagerPassword;
	}

	public void setSystemManagerPassword(String systemManagerPassword) {
		this.systemManagerPassword = systemManagerPassword;
	}

	public String getSecurityManagerPassword() {
		if (StringUtils.isEmpty(this._securityManagerPassword)) {
			String password = StringUtils.isBlank(this.securityManagerPassword) ? initPassword
					: this.securityManagerPassword;
			this._securityManagerPassword = Crypto.plainText(password);
		}
		return this._securityManagerPassword;
	}

	public void setSecurityManagerPassword(String securityManagerPassword) {
		this.securityManagerPassword = securityManagerPassword;
	}

	public String getAuditManagerPassword() {
		if (StringUtils.isEmpty(this._auditManagerPassword)) {
			String password = StringUtils.isBlank(this.auditManagerPassword) ? initPassword : this.auditManagerPassword;
			this._auditManagerPassword = Crypto.plainText(password);
		}
		return this._auditManagerPassword;
	}

	public void setAuditManagerPassword(String auditManagerPassword) {
		this.auditManagerPassword = auditManagerPassword;
	}
}
