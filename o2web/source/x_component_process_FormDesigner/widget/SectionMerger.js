MWF.xApplication.process.FormDesigner.widget = MWF.xApplication.process.FormDesigner.widget || {};
MWF.require("MWF.widget.ScriptArea", null, false);
MWF.xApplication.process.FormDesigner.widget.SectionMerger = new Class({
	Implements: [Options, Events],
	Extends: MWF.widget.Common,
	options: {
		"style": "default",
		"maxObj": document.body
	},
	initialize: function(node, property, options){

		this.setOptions(options);
		this.node = $(node);
		this.property = property;
        this.app = this.property.designer;
		this.designer = this.property.designer;
		this.form = this.property.form;
		this.module = this.property.module;
		
		// this.path = "../x_component_process_FormDesigner/widget/$SectionMerger/";
		// this.cssPath = "../x_component_process_FormDesigner/widget/$SectionMerger/"+this.options.style+"/css.wcss";
		this.lp = this.app.lp.propertyTemplate;
		// this._loadCss();
	},
	
	load: function(data){
		var _self = this;

		this.data = data;

        this.node.set("html", this.getHtml());

        this.readArea = this.node.getElement(".sectionMergeReadArea");
        this.readDefaultArea = this.node.getElement(".sectionMergeReadDefaultArea");
		this.readScriptArea = this.node.getElement(".sectionMergeReadScriptArea");

		this.readWithSectionKeyArea = this.node.getElement(".readWithSectionKeyArea");
		this.sectionKeyScriptArea = this.node.getElement(".sectionKeyScriptArea");

		this.editArea = this.node.getElement(".sectionMergeEditArea");
		this.editScriptArea = this.node.getElement(".sectionMergeEditScriptArea");
		this.mergeTypeEditTable = this.node.getElement("[item='mergeTypeEditTable']");

		this.sortScriptArea = this.node.getElement(".sectionMergeSortScriptArea");

		var lp = this.lp;
		var moduleName = this.module.moduleName;

		this.hasEditDefaultModuleList = ["textfield", "checkbox", "datatable", "datatemplate", "org", "textarea", "elautocomplete", "elcheckbox", "elinput"];

		debugger;

		if( o2.typeOf( this.data.sectionMerge ) === "null" )this.data.sectionMerge = "none";
		if( o2.typeOf( this.data.mergeTypeRead ) === "null" )this.data.mergeTypeRead = "default";
		if( o2.typeOf( this.data.showSectionKey ) === "null" )this.data.showSectionKey = true;
		if( o2.typeOf( this.data.sectionKey ) === "null" )this.data.sectionKey = "person";
		if( o2.typeOf( this.data.mergeTypeEdit ) === "null" ){
			if( !this.hasEditDefaultModuleList.contains( moduleName ) ){
				this.data.mergeTypeEdit = "script"
			}else if(["number", "elnumber"].contains(moduleName)){
				this.data.mergeTypeEdit = "amount"
			}else{
				this.data.mergeTypeEdit = "default"
			}
		}

		MWF.xDesktop.requireApp("Template", "MForm", function () {
			this.form = new MForm(this.node, this.data, {
				isEdited: true,
				style : "",
				hasColon : true,
				itemTemplate: {
					sectionMerge: { type : "radio", selectValue: ["none", "read", "edit"], selectText: [lp.none, lp.mergeDisplay, lp.mergeEdit], event: {
							change: function (it, ev) {
								_self.property.setRadioValue("sectionMerge", this);
								_self.checkShow()
							}
						}},
					mergeTypeRead: { type : "radio", className: "editTableRadio",
						selectValue: function(){
							return  ["number", "elnumber"].contains(moduleName) ? ["default", "amount", "average", "script"] : ["default", "script"]
						},
						selectText: function () {
							return ["number", "elnumber"].contains(moduleName) ? [lp.default, lp.amountValue, lp.averageValue, lp.script] : [lp.default, lp.script]
						},
						event: {
							change: function (it) {
								_self.property.setRadioValue("mergeTypeRead", this);
								_self.checkShow()
							}
						}},
					showSectionKey: { type : "radio", className: "editTableRadio", selectValue: ["true", "false"], selectText: [lp.yes, lp.no], event: {
							change: function (it) {
								_self.property.setRadioValue("showSectionKey", this);
								_self.checkShow()
							}
						}},
					keyContentSeparator: { tType : "text" , className: "editTableInput", event: {
							change: function (it) {
								this.property.setValue("keyContentSeparator", it.getValue(), this);
							}
						}},
					sectionKey: { type : "radio", selectValue: ["person", "unit", "textValue", "script"], selectText: [lp.person, lp.unit, lp.textValue1, lp.script], event: {
							change: function (it) {
								_self.property.setRadioValue("sectionKey", this);
								_self.checkShow()
							}
						}},
					mergeTypeEdit: { type : "radio", className: "editTableRadio",
						selectValue: function(){
							return ["number", "elnumber"].contains(moduleName) ? ["amount", "average", "script"] : ["default", "script"]
						},
						selectText: function () {
							return ["number", "elnumber"].contains(moduleName) ? [lp.amountValue, lp.averageValue, lp.script] : [lp.default, lp.script]
						},
						event: {
							change: function (it) {
								_self.property.setRadioValue("mergeTypeEdit", this);
								_self.checkShow()
							}
						}},
				}
			}, this.app, this.form.css);
			this.form.load();
			// this.setEditNodeStyles( this.node );
			this.loadMaplist();
			this.loadScriptArea();
			this.checkShow( this.data );
		}.bind(this), true);
	},
	checkShow: function(d){
		debugger;
		if( !d )d = this.data;
		var _self = this;
		var showCondition = {
			"readArea": function() {
				return d.sectionMerge==='read'
			},
			"readDefaultArea": function () {
				return d.mergeTypeRead==='default' || !d.mergeTypeRead;
			},
			"readWithSectionKeyArea": function () {
				return !!d.showSectionKey;
			},
			"sectionKeyScriptArea": function () {
				return d.sectionKey === "script";
			},
			"readScriptArea": function () {
				return d.sectionMerge==='read' && d.mergeTypeRead==='script';
			},
			"editArea": function() {
				return d.sectionMerge==='edit';
			},
			"editScriptArea": function () {
				return d.sectionMerge==='edit' && d.mergeTypeEdit === 'script';
			},
			"sortScriptArea": function () {
				return ( d.sectionMerge==='read' && d.mergeTypeRead === "default" ) ||
					(d.sectionMerge==='edit' && d.mergeTypeEdit === "default")
			},
			"mergeTypeEditTable" : function () {
				return _self.hasEditDefaultModuleList.contains( _self.module.moduleName ) ||
					["number", "elnumber"].contains( _self.module.moduleName );
			}
		};
		for( var key in showCondition ){
			if( showCondition[key]() ){
				this[key].setStyle("display", "");
			}else{
				this[key].hide()
			}
		}
	},
	getHtml: function(){
		return '<table width="100%" border="0" cellpadding="5" cellspacing="0" class="editTable">' +
			'    <tr>' +
			'        <td class="editTableTitle">'+this.lp.enableSectionMerge+':</td>' +
			'        <td class="editTableValue" item="sectionMerge"></td>' +
			'    </tr>' +
			'</table>' +
			'<div class="sectionMergeReadArea">' +
			'    <table width="100%" border="0" cellpadding="5" cellspacing="0" class="editTable">' +
			'        <tr>' +
			'            <td class="editTableTitle">'+this.lp.mergeType+':</td>' +
			'            <td class="editTableValue" item="mergeTypeRead"></td>' +
			'        </tr>' +
			'    </table>' +
			'    <div class="sectionMergeReadDefaultArea">' +
			'        <table width="100%" border="0" cellpadding="5" cellspacing="0" class="editTable">' +
			'            <tr>' +
			'                <td class="editTableTitle">'+this.lp.showSectionKey+':</td>' +
			'                <td class="editTableValue" item="showSectionKey"></td>' +
			'            </tr>' +
			'        </table>' +
			'        <div class="MWFMaplist" name="sectionNodeStyles" title="'+this.lp.sectionNodeStyles+'"></div>' +
			'        <div class="MWFMaplist" name="sectionContentStyles" title="'+this.lp.sectionContentStyles+'"></div>' +
			'        <div class="readWithSectionKeyArea">' +
			'            <div class="MWFMaplist" name="sectionKeyStyles" title="'+this.lp.sectionKeystyles+'"></div>' +
			'            <table width="100%" border="0" cellpadding="5" cellspacing="0" class="editTable">' +
			'                <tr>' +
			'                    <td class="editTableTitle">'+this.lp.separator+':</td>' +
			'                    <td class="editTableValue" item="keyContentSeparator"></td>' +
			'                </tr>' +
			'                <tr>' +
			'                    <td class="editTableTitle">'+this.lp.sectionKey+':</td>' +
			'                    <td class="editTableValue" item="sectionKey"></td>' +
			'                </tr>' +
			'            </table>' +
			'            <div class="sectionKeyScriptArea">' +
			'                <div class="MWFScriptArea" name="sectionKeyScript" title="'+this.lp.sectionKeyScript+' (S)"></div>' +
			'            </div>' +
			'        </div>' +
			'    </div>' +
			'    <div class="sectionMergeReadScriptArea">' +
			'        <div class="MWFScriptArea" name="sectionMergeReadScript" title="'+this.lp.sectionMergeReadScript+' (S)"></div>' +
			'        <div style="padding: 10px;color:#999">返回需展现的html</div>' +
			'    </div>    ' +
			'</div>' +
			'<div class="sectionMergeEditArea">' +
			'    <table width="100%" border="0" cellpadding="5" cellspacing="0" class="editTable" item="mergeTypeEditTable">' +
			'        <tr>' +
			'            <td class="editTableTitle">'+this.lp.mergeType+':</td>' +
			'            <td class="editTableValue" item="mergeTypeEdit"></td>' +
			'        </tr>' +
			'    </table>' +
			'    <div class="sectionMergeEditScriptArea">' +
			'        <div class="MWFScriptArea" name="sectionMergeEditScript" title="'+this.lp.sectionMergeEditScript+' (S)"></div>' +
			'        <div style="padding: 10px;color:#999">返回删除区段后合并的数据</div>' +
			'    </div>' +
			'</div>' +
			'<div class="sectionMergeSortScriptArea">' +
			'    <div class="MWFScriptArea" name="sectionMergeSortScript" title="'+this.lp.sectionMergeSortScript+' (S)"></div>' +
			'    <div style="padding: 10px;color:#999">排序脚本通过this.envent.a和this.evnet.b获取数据，处理后返回正数表示升序，负数表示降序。this.envent.a和this.envent.b值如: <br/>' +
			'        { <br/>' +
			'        key: "张三@zhangsan@P", //区段值 <br/>' +
			'        data: "内容" //字段内容 <br/>' +
			'        }' +
			'    </div>' +
			'</div>'
	},
	setEditNodeStyles: function(node){
		var nodes = node.getChildren();
		if (nodes.length){
			nodes.each(function(el){
				var cName = el.get("class");
				if (cName){
					if (this.form.css[cName]) el.setStyles(this.form.css[cName]);
				}
				this.setEditNodeStyles(el);
			}.bind(this));
		}
	},
	loadMaplist: function(){
		var maplists = this.node.getElements(".MWFMaplist");
		maplists.each(function(node){
			var title = node.get("title");
			var name = node.get("name");
			var lName = name.toLowerCase();
			var collapse = node.get("collapse");
			var mapObj = this.data[name];
			if (!mapObj) mapObj = {};
			MWF.require("MWF.widget.Maplist", function(){
				node.empty();
				var maplist = new MWF.widget.Maplist(node, {
					"title": title,
					"collapse": (collapse) ? true : false,
					"onChange": function(){
						//this.data[name] = maplist.toJson();
						//
						var oldData = this.data[name];
						this.property.changeJsonDate(name, maplist.toJson());
						this.property.changeStyle(name, oldData);
						this.property.changeData(name);
					}.bind(this),
					"onDelete": function(key){
						debugger;

						this.module.deletePropertiesOrStyles(name, key);
					}.bind(this),
					"isProperty": (lName.contains("properties") || lName.contains("property") || lName.contains("attribute"))
				});
				maplist.load(mapObj);
				this.property.maplists[name] = maplist;
			}.bind(this));
		}.bind(this));
	},
	loadScriptArea: function(){
		var scriptAreas = this.node.getElements(".MWFScriptArea");
		var formulaAreas = this.node.getElements(".MWFFormulaArea");
		this.loadScriptEditor(scriptAreas);
		this.loadScriptEditor(formulaAreas, "formula");
	},
	loadScriptEditor: function(scriptAreas, style){
		scriptAreas.each(function(node){
			var title = node.get("title");
			var name = node.get("name");
			if (!this.data[name]) this.data[name] = {"code": "", "html": ""};
			var scriptContent = this.data[name];

			var mode = node.dataset["mode"];
			MWF.require("MWF.widget.ScriptArea", function(){
				var scriptArea = new MWF.widget.ScriptArea(node, {
					"title": title,
					"mode": mode || "javascript",
					//"maxObj": this.propertyNode.parentElement.parentElement.parentElement,
					"maxObj": this.designer.formContentNode || this.designer.pageContentNode,
					"onChange": function(){
						//this.data[name] = scriptArea.toJson();
						if (!this.data[name]){
							this.data[name] = {"code": "", "html": ""};
							if (this.module.form.scriptDesigner) this.module.form.scriptDesigner.addScriptItem(this.data[name], "code", this.data, name);
						}
						var json = scriptArea.toJson();
						this.data[name].code = json.code;
						//this.data[name].html = json.html;
					}.bind(this),
					"onSave": function(){
						this.designer.saveForm();
					}.bind(this),
					"style": style || "default",
					"runtime": "web"
				});
				scriptArea.load(scriptContent);
			}.bind(this));

		}.bind(this));
	}
	
});

