import React, { useState } from "react";
import TopBar from "../components/TopBar";
import McpServersView from "./mcpServers/McpServersView";
import AgentView from "./agents/AgentView";
import LLMView from "./llm/LLMView";
import { Box, Stack, Typography } from "@mui/material";
import { ImportMcpServerDialog } from "../components/ImportMcpServerDialog"; // Asegúrate de ajustar la ruta si es necesario

// Ajuste para importar el logo
import logo from "../../assets/images/registry.png"; // Cambia la ruta a la que subiste el logo

export default function HomePage() {
  const [view, setView] = useState("servers");
  const [importDialogOpen, setImportDialogOpen] = useState(false);

  // Función para manejar cuando los datos se importan correctamente
  const onImported = (res) => {
    console.log("Import result:", res);
    // Aquí puedes refrescar la lista de servers, o cualquier otra acción
    // Ejemplo: refetchServers(); si tienes un método de refetch.
  };

  return (
    <>
      <Box sx={{ p: 2, display: "flex", alignItems: "flex-start", justifyContent: "space-between", flexDirection: "column" }}>
        {/* Título y logo */}
        <Stack direction="column" spacing={1} alignItems="flex-start">
          <img src={logo} alt="Agentic Registry Logo" style={{ height: 100 }} />
          <Typography variant="h6" fontWeight={900}>
            A registry for all your Agentic needs. You can manage your various MCP Servers and AI agents from here.
          </Typography>
        </Stack>
      </Box>
      <TopBar view={view} setView={setView} />

      {/* Mostrar la vista correspondiente */}
      {view === "servers" && <McpServersView />}
      {view === "agents" && <AgentView />}
      {view === "llm" && <LLMView />}

      {/* Dialog de Importación de MCP Server */}
      <ImportMcpServerDialog
        open={importDialogOpen}
        onClose={() => setImportDialogOpen(false)}
        onImported={onImported}
      />
    </>
  );
}
