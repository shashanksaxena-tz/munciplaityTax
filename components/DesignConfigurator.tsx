import React, { useState } from 'react';
import { Palette, Save, RotateCcw, Download, Upload, Eye, EyeOff } from 'lucide-react';
import { designSystem } from '../config/design-system';

interface DesignConfiguratorProps {
  onClose?: () => void;
}

interface ColorConfig {
  [key: string]: string;
}

export const DesignConfigurator: React.FC<DesignConfiguratorProps> = ({ onClose }) => {
  const [config, setConfig] = useState(designSystem);
  const [showPreview, setShowPreview] = useState(true);
  const [activeTab, setActiveTab] = useState<'colors' | 'typography' | 'spacing'>('colors');

  const handleColorChange = (path: string[], value: string) => {
    setConfig(prev => {
      const newConfig = { ...prev };
      let current: any = newConfig;
      for (let i = 0; i < path.length - 1; i++) {
        current = current[path[i]];
      }
      current[path[path.length - 1]] = value;
      return newConfig;
    });
  };

  const handleReset = () => {
    setConfig(designSystem);
  };

  const handleExport = () => {
    const dataStr = JSON.stringify(config, null, 2);
    const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
    const exportFileDefaultName = 'design-system-config.json';
    
    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  };

  const handleImport = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const imported = JSON.parse(e.target?.result as string);
          setConfig(imported);
        } catch (error) {
          alert('Invalid configuration file');
        }
      };
      reader.readAsText(file);
    }
  };

  const handleSave = () => {
    // Generate the updated design-system.ts file content
    const configStr = `export const designSystem = ${JSON.stringify(config, null, 2)};`;
    
    // Copy to clipboard
    navigator.clipboard.writeText(configStr);
    alert('Configuration copied to clipboard! Paste it into config/design-system.ts');
  };

  const ColorInput = ({ label, value, onChange, path }: { label: string; value: string; onChange: (path: string[], value: string) => void; path: string[] }) => (
    <div className="flex items-center gap-3 py-2">
      <input
        type="color"
        value={value}
        onChange={(e) => onChange(path, e.target.value)}
        className="w-12 h-12 rounded-lg border-2 border-[#dcdede] cursor-pointer"
      />
      <div className="flex-1">
        <label className="text-sm font-medium text-[#0f1012]">{label}</label>
        <input
          type="text"
          value={value}
          onChange={(e) => onChange(path, e.target.value)}
          className="w-full mt-1 px-3 py-1 text-sm border border-[#dcdede] rounded-lg focus:border-[#970bed] focus:ring-[#970bed]/20 focus:ring-2 outline-none font-mono"
        />
      </div>
    </div>
  );

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl border border-[#dcdede] w-full max-w-6xl max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#dcdede]">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-r from-[#970bed] to-[#469fe8] rounded-lg">
              <Palette className="w-6 h-6 text-white" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-[#0f1012]">Design System Configurator</h2>
              <p className="text-sm text-[#5d6567]">Customize the entire application's look and feel</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-[#fbfbfb] rounded-lg transition-colors"
          >
            <svg className="w-6 h-6 text-[#5d6567]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Actions Bar */}
        <div className="flex items-center gap-2 px-6 py-3 border-b border-[#dcdede] bg-[#fbfbfb]">
          <button
            onClick={handleSave}
            className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-lg font-medium transition-all"
          >
            <Save className="w-4 h-4" />
            Save & Copy
          </button>
          <button
            onClick={handleReset}
            className="flex items-center gap-2 px-4 py-2 border border-[#dcdede] text-[#5d6567] hover:bg-white rounded-lg font-medium transition-all"
          >
            <RotateCcw className="w-4 h-4" />
            Reset
          </button>
          <button
            onClick={handleExport}
            className="flex items-center gap-2 px-4 py-2 border border-[#dcdede] text-[#5d6567] hover:bg-white rounded-lg font-medium transition-all"
          >
            <Download className="w-4 h-4" />
            Export JSON
          </button>
          <label className="flex items-center gap-2 px-4 py-2 border border-[#dcdede] text-[#5d6567] hover:bg-white rounded-lg font-medium transition-all cursor-pointer">
            <Upload className="w-4 h-4" />
            Import JSON
            <input type="file" accept=".json" onChange={handleImport} className="hidden" />
          </label>
          <div className="flex-1" />
          <button
            onClick={() => setShowPreview(!showPreview)}
            className="flex items-center gap-2 px-4 py-2 border border-[#dcdede] text-[#5d6567] hover:bg-white rounded-lg font-medium transition-all"
          >
            {showPreview ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            {showPreview ? 'Hide' : 'Show'} Preview
          </button>
        </div>

        {/* Main Content */}
        <div className="flex-1 overflow-hidden flex">
          {/* Left Panel - Configuration */}
          <div className={`${showPreview ? 'w-1/2' : 'w-full'} border-r border-[#dcdede] flex flex-col`}>
            {/* Tabs */}
            <div className="flex border-b border-[#dcdede] bg-[#fbfbfb]">
              {['colors', 'typography', 'spacing'].map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab as any)}
                  className={`flex-1 px-4 py-3 font-medium capitalize transition-all ${
                    activeTab === tab
                      ? 'text-[#970bed] border-b-2 border-[#970bed] bg-white'
                      : 'text-[#5d6567] hover:bg-white'
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>

            {/* Configuration Panel */}
            <div className="flex-1 overflow-y-auto p-6">
              {activeTab === 'colors' && (
                <div className="space-y-6">
                  {/* Primary Colors */}
                  <div>
                    <h3 className="text-lg font-bold text-[#0f1012] mb-4">Primary Colors</h3>
                    <div className="space-y-2">
                      <ColorInput
                        label="Purple (Gradient Start)"
                        value={config.colors.primary.purple}
                        onChange={handleColorChange}
                        path={['colors', 'primary', 'purple']}
                      />
                      <ColorInput
                        label="Blue (Gradient End)"
                        value={config.colors.primary.blue}
                        onChange={handleColorChange}
                        path={['colors', 'primary', 'blue']}
                      />
                      <ColorInput
                        label="Purple Dark (Hover)"
                        value={config.colors.primary.purpleDark}
                        onChange={handleColorChange}
                        path={['colors', 'primary', 'purpleDark']}
                      />
                      <ColorInput
                        label="Blue Dark (Hover)"
                        value={config.colors.primary.blueDark}
                        onChange={handleColorChange}
                        path={['colors', 'primary', 'blueDark']}
                      />
                    </div>
                  </div>

                  {/* Text Colors */}
                  <div>
                    <h3 className="text-lg font-bold text-[#0f1012] mb-4">Text Colors</h3>
                    <div className="space-y-2">
                      <ColorInput
                        label="Heading"
                        value={config.colors.text.heading}
                        onChange={handleColorChange}
                        path={['colors', 'text', 'heading']}
                      />
                      <ColorInput
                        label="Label"
                        value={config.colors.text.label}
                        onChange={handleColorChange}
                        path={['colors', 'text', 'label']}
                      />
                      <ColorInput
                        label="Body"
                        value={config.colors.text.body}
                        onChange={handleColorChange}
                        path={['colors', 'text', 'body']}
                      />
                      <ColorInput
                        label="Muted"
                        value={config.colors.text.muted}
                        onChange={handleColorChange}
                        path={['colors', 'text', 'muted']}
                      />
                    </div>
                  </div>

                  {/* Status Colors */}
                  <div>
                    <h3 className="text-lg font-bold text-[#0f1012] mb-4">Status Colors</h3>
                    <div className="space-y-2">
                      <ColorInput
                        label="Success"
                        value={config.colors.status.success}
                        onChange={handleColorChange}
                        path={['colors', 'status', 'success']}
                      />
                      <ColorInput
                        label="Error"
                        value={config.colors.status.error}
                        onChange={handleColorChange}
                        path={['colors', 'status', 'error']}
                      />
                      <ColorInput
                        label="Warning"
                        value={config.colors.status.warning}
                        onChange={handleColorChange}
                        path={['colors', 'status', 'warning']}
                      />
                      <ColorInput
                        label="Info"
                        value={config.colors.status.info}
                        onChange={handleColorChange}
                        path={['colors', 'status', 'info']}
                      />
                    </div>
                  </div>

                  {/* Backgrounds */}
                  <div>
                    <h3 className="text-lg font-bold text-[#0f1012] mb-4">Backgrounds</h3>
                    <div className="space-y-2">
                      <ColorInput
                        label="Off-White (Cards)"
                        value={config.colors.background.offWhite}
                        onChange={handleColorChange}
                        path={['colors', 'background', 'offWhite']}
                      />
                      <ColorInput
                        label="Grey"
                        value={config.colors.background.grey}
                        onChange={handleColorChange}
                        path={['colors', 'background', 'grey']}
                      />
                    </div>
                  </div>

                  {/* Borders */}
                  <div>
                    <h3 className="text-lg font-bold text-[#0f1012] mb-4">Borders</h3>
                    <div className="space-y-2">
                      <ColorInput
                        label="Default Border"
                        value={config.colors.border.default}
                        onChange={handleColorChange}
                        path={['colors', 'border', 'default']}
                      />
                      <ColorInput
                        label="Focus Border"
                        value={config.colors.border.focus}
                        onChange={handleColorChange}
                        path={['colors', 'border', 'focus']}
                      />
                    </div>
                  </div>
                </div>
              )}

              {activeTab === 'typography' && (
                <div className="space-y-6">
                  <div className="text-center text-[#5d6567]">
                    Typography configuration (font sizes, weights) coming soon...
                  </div>
                </div>
              )}

              {activeTab === 'spacing' && (
                <div className="space-y-6">
                  <div className="text-center text-[#5d6567]">
                    Spacing configuration (margins, padding, border radius) coming soon...
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Right Panel - Live Preview */}
          {showPreview && (
            <div className="w-1/2 overflow-y-auto p-6 bg-[#f8f9fa]">
              <h3 className="text-lg font-bold text-[#0f1012] mb-4">Live Preview</h3>
              <div className="space-y-4">
                {/* Button Preview */}
                <div className="bg-white rounded-xl p-4 border border-[#dcdede]">
                  <h4 className="text-sm font-semibold text-[#0f1012] mb-3">Buttons</h4>
                  <div className="flex flex-wrap gap-2">
                    <button
                      className="px-6 py-3 rounded-xl font-bold text-white transition-all"
                      style={{
                        background: `linear-gradient(to right, ${config.colors.primary.purple}, ${config.colors.primary.blue})`,
                      }}
                    >
                      Primary Button
                    </button>
                    <button
                      className="px-6 py-3 rounded-xl font-medium transition-all"
                      style={{
                        border: `1px solid ${config.colors.border.default}`,
                        color: config.colors.text.body,
                      }}
                    >
                      Secondary Button
                    </button>
                  </div>
                </div>

                {/* Form Input Preview */}
                <div className="bg-white rounded-xl p-4 border border-[#dcdede]">
                  <h4 className="text-sm font-semibold text-[#0f1012] mb-3">Form Input</h4>
                  <input
                    type="text"
                    placeholder="Enter text..."
                    className="w-full px-4 py-3 rounded-xl outline-none transition-all"
                    style={{
                      border: `1px solid ${config.colors.border.default}`,
                      color: config.colors.text.body,
                    }}
                  />
                </div>

                {/* Badges Preview */}
                <div className="bg-white rounded-xl p-4 border border-[#dcdede]">
                  <h4 className="text-sm font-semibold text-[#0f1012] mb-3">Status Badges</h4>
                  <div className="flex flex-wrap gap-2">
                    <span
                      className="px-3 py-1 rounded-full text-xs font-medium"
                      style={{
                        backgroundColor: `${config.colors.tints.greenBg}`,
                        color: config.colors.status.success,
                      }}
                    >
                      Success
                    </span>
                    <span
                      className="px-3 py-1 rounded-full text-xs font-medium"
                      style={{
                        backgroundColor: `${config.colors.status.error}1a`,
                        color: config.colors.status.error,
                      }}
                    >
                      Error
                    </span>
                    <span
                      className="px-3 py-1 rounded-full text-xs font-medium"
                      style={{
                        backgroundColor: `${config.colors.status.warning}1a`,
                        color: config.colors.status.warning,
                      }}
                    >
                      Warning
                    </span>
                  </div>
                </div>

                {/* Card Preview */}
                <div
                  className="rounded-xl p-4"
                  style={{
                    backgroundColor: config.colors.background.offWhite,
                    border: `1px solid ${config.colors.border.default}`,
                  }}
                >
                  <h4 className="text-lg font-bold mb-2" style={{ color: config.colors.text.heading }}>
                    Card Component
                  </h4>
                  <p className="text-sm" style={{ color: config.colors.text.body }}>
                    This is a preview of how cards will look with your selected colors.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DesignConfigurator;
