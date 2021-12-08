MWF.xApplication.process.FormDesigner.Module = MWF.xApplication.process.FormDesigner.Module || {};
MWF.xDesktop.requireApp("process.FormDesigner", "Module.$ElElement", null, false);
MWF.xApplication.process.FormDesigner.Module.Eldate = MWF.FCEldate = new Class({
	Extends: MWF.FC$ElElement,
	Implements: [Options, Events],
	options: {
		"style": "default",
		"propertyPath": "../x_component_process_FormDesigner/Module/Eldate/eldate.html"
	},

	_initModuleType: function(){
		this.className = "Eldate";
		this.moduleType = "element";
		this.moduleName = "eldate";
	},
	_createElementHtml: function(){

		var html = "<el-date-picker";
		html += " :placeholder=\"description\"";
		html += " :type=\"inputType\"";
		html += " :maxlength=\"maxlength\"";
		html += " :show-word-limit=\"showWordLimit\"";
		html += " :clearable=\"clearable\"";
		html += " :show-password=\"showPassword\"";
		html += " :size=\"size\"";
		html += " :rows=\"rows\"";
		html += " :resize=\"resize\"";
		html += " :prefix-icon=\"prefixIcon\"";
		html += " :suffix-icon=\"suffixIcon\"";

		if (this.json.elProperties){
			Object.keys(this.json.elProperties).forEach(function(k){
				if (this.json.elProperties[k]) html += " "+k+"=\""+this.json.elProperties[k]+"\"";
			}, this);
		}

		html += " :style=\"elStyles\"";
		html += " :value=\"id\">";
		if (this.json.vueSlot) html += this.json.vueSlot;
		html += "</el-date-picker>";
		return html;
	},
	_createCopyNode: function(){
		this.copyNode = new Element("div", {
			"styles": this.css.moduleNodeShow
		});
		this.copyNode.addEvent("selectstart", function(){
			return false;
		});
	},
	_getCopyNode: function(){
		if (!this.copyNode) this._createCopyNode();
		this.copyNode.setStyle("display", "inline-block");
		return this.copyNode;
	},
	setPropertyName: function(){
		if (this.json.name){
			var input = this.node.getElement("input");
			if (input) input.set("value", this.json.name);
		}
	},
	setPropertyId: function(){
		if (!this.json.name){
			var input = this.node.getElement("input");
			if (input) input.set("value", this.json.id);
		}
	}
});
