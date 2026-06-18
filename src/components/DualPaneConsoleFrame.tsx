import React from "react";

interface ConsolePanelProps {
  title?: React.ReactNode;
  className?: string;
  theme?: "green" | "purple";
  children: React.ReactNode;
}

export function ConsolePanel({
  title,
  className = "",
  theme = "green",
  children
}: ConsolePanelProps) {
  const isPurple = theme === "purple";
  const themeBorder = isPurple 
    ? "border-purple-900/40" 
    : "border-green-900/40";

  return (
    <div className={`bg-neutral-900/20 p-4 rounded flex flex-col justify-between relative overflow-y-auto custom-pox-scrollbar min-h-[300px] border transition-all ${themeBorder} ${className}`}>
      {title && (
        <div className="flex justify-between items-center h-8 mb-2 select-none">
          {typeof title === "string" ? (
            <h2 className="text-xs font-bold text-white tracking-wider uppercase">
              {title}
            </h2>
          ) : (
            title
          )}
        </div>
      )}
      <div className="flex-grow flex flex-col justify-between min-h-0">
        {children}
      </div>
    </div>
  );
}

interface DualPaneConsoleFrameProps {
  theme?: "green" | "purple";
  flavorTitle: string;
  statusElement?: React.ReactNode;
  isSinglePane?: boolean;
  children: React.ReactNode;
}

export default function DualPaneConsoleFrame({
  theme = "green",
  flavorTitle,
  statusElement,
  isSinglePane = false,
  children
}: DualPaneConsoleFrameProps) {
  const isPurple = theme === "purple";
  const themeHeaderColor = isPurple 
    ? "text-purple-400" 
    : "text-green-700";

  return (
    <div className="flex flex-col h-full space-y-3">
      {/* Universal Header */}
      <div className={`flex justify-between items-center mb-1 text-[10px] uppercase ${themeHeaderColor}`}>
        <span className="font-sans text-[10px] font-bold tracking-wider uppercase">
          {flavorTitle}
        </span>
        {statusElement}
      </div>

      {/* Grid container */}
      <div className={`grid grid-cols-1 ${isSinglePane ? "" : "md:grid-cols-2"} gap-4 flex-grow overflow-hidden`}>
        {children}
      </div>
    </div>
  );
}
