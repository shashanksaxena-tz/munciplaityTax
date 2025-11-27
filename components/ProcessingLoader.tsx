
import React, { useState, useEffect } from 'react';
import { Loader2, Lightbulb, FileSearch, BrainCircuit, Sparkles } from 'lucide-react';

const FACTS = [
  "Did you know? Dublin's local income tax rate is currently 2.0%.",
  "Fact: You can receive up to a 100% credit for taxes paid to other municipalities, capped at 2.0%.",
  "Tax Tip: W-2G Gambling Winnings are fully taxable in Dublin without deduction for losses.",
  "Filing Deadline: Your Dublin return is generally due on April 15th, matching the Federal deadline.",
  "Rule: Qualifying wages are typically the highest of Box 5 (Medicare) or Box 18 (Local) wages.",
  "History: The City of Dublin uses income tax revenue to fund capital improvements and safety services.",
  "Schedule E: Losses from one rental property can offset gains from another rental property in Dublin calculations.",
  "JEDD: If you work in a Joint Economic Development District, special tax rules may apply to your withholding."
];

const STATUS_MESSAGES = [
  "Scanning document structure...",
  "Identifying distinct tax forms...",
  "Filtering out instruction pages...",
  "Extracting W-2 Wage details...",
  "Parsing Schedule business income...",
  "Analyzing credits for other cities...",
  "Validating extracted amounts..."
];

export const ProcessingLoader: React.FC = () => {
  const [factIndex, setFactIndex] = useState(0);
  const [statusIndex, setStatusIndex] = useState(0);
  const [progress, setProgress] = useState(0);

  // Cycle facts every 5 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      setFactIndex((prev) => (prev + 1) % FACTS.length);
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  // Cycle status every 1.5 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      setStatusIndex((prev) => (prev + 1) % STATUS_MESSAGES.length);
    }, 1500);
    return () => clearInterval(interval);
  }, []);

  // Fake progress bar
  useEffect(() => {
    const interval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 90) return prev; // Stall at 90% until done
        return prev + Math.random() * 5;
      });
    }, 500);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="w-full max-w-2xl mx-auto bg-white rounded-2xl shadow-xl border border-indigo-100 overflow-hidden p-8 flex flex-col items-center text-center animate-fadeIn">
      
      <div className="relative mb-8">
        <div className="absolute inset-0 bg-indigo-500/20 blur-xl rounded-full animate-pulse"></div>
        <div className="relative bg-white p-4 rounded-full shadow-sm border border-indigo-50">
          <Loader2 className="w-12 h-12 text-indigo-600 animate-spin" />
        </div>
        <div className="absolute -right-2 -top-2 bg-white p-1.5 rounded-full shadow border border-indigo-50 animate-bounce delay-100">
           <BrainCircuit className="w-5 h-5 text-purple-500" />
        </div>
        <div className="absolute -left-2 -bottom-2 bg-white p-1.5 rounded-full shadow border border-indigo-50 animate-bounce delay-300">
           <FileSearch className="w-5 h-5 text-blue-500" />
        </div>
      </div>

      <h2 className="text-2xl font-bold text-slate-800 mb-2">Smart Extraction in Progress</h2>
      <p className="text-indigo-600 font-medium min-h-[24px] transition-all duration-300">
        {STATUS_MESSAGES[statusIndex]}
      </p>

      {/* Progress Bar */}
      <div className="w-full max-w-md h-2 bg-slate-100 rounded-full mt-6 mb-8 overflow-hidden relative">
        <div 
          className="absolute top-0 left-0 h-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-500 ease-out"
          style={{ width: `${progress}%` }}
        ></div>
      </div>

      {/* Fact Card */}
      <div className="w-full bg-gradient-to-br from-slate-50 to-indigo-50/50 border border-indigo-100 rounded-xl p-6 relative">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-white border border-indigo-100 px-3 py-1 rounded-full shadow-sm flex items-center gap-1.5">
          <Lightbulb className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
          <span className="text-xs font-bold text-slate-600 uppercase tracking-wider">Muni Tax Fact</span>
        </div>
        
        <div className="min-h-[60px] flex items-center justify-center">
          <p className="text-slate-700 text-sm leading-relaxed italic transition-opacity duration-500 animate-fadeIn key={factIndex}">
            "{FACTS[factIndex]}"
          </p>
        </div>
      </div>

      <div className="mt-6 flex items-center gap-2 text-xs text-slate-400">
        <Sparkles className="w-3 h-3" />
        <span>Powered by Gemini AI Model 2.5 Flash</span>
      </div>

    </div>
  );
};
