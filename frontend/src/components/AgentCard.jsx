import React, { useState } from "react";
import {
  Card, CardContent, CardActions,
  Typography, Stack, Chip, Button,
  IconButton, Tooltip, Divider, Box
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import WifiTetheringIcon from "@mui/icons-material/WifiTethering";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import CodeIcon from "@mui/icons-material/Code";  
import { getAgentDefinition } from "../api/agents";  // Asegúrate de importar el servicio adecuado
import AgentDefinitionDialog from "./AgentDefinitionDialog"; // Asegúrate de importar el componente de diálogo

const getStatusLabel = (status) => {
  switch (status) {
    case "ACTIVE":
      return { label: "Active", color: "success" };
    case "FAILED":
      return { label: "Failed", color: "error" };
    case "DISABLED":
      return { label: "Disabled", color: "default" };
    case "INACTIVE":
      return { label: "Inactive", color: "error" };
  }
};

const formatDate = (timestamp) => {
  if (!timestamp) return "—";
  try {
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) return "—";
    return date.toLocaleString();
  } catch {
    return "—";
  }
};

const AgentCard = ({ agent, onEdit, onDelete, onTest }) => {
  const { agentId, name, description, version, updatedTs, status, metadata } = agent;
  const { label, color } = getStatusLabel(status);

  const [openDialog, setOpenDialog] = useState(false);
  const [agentDefinition, setAgentDefinition] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Función para abrir el diálogo y obtener la definición del agente
  const handleViewAgentDefinition = async () => {
    setLoading(true);
    setError(null);  // Limpiar cualquier error previo
    try {
      const agentDetails = await getAgentDefinition(agentId);  // Usar el servicio para obtener detalles del agente
   console.log(agentDetails); 
      setAgentDefinition(agentDetails);  // Establecer la definición del agente
      setOpenDialog(true);  // Abrir el diálogo
    } catch (err) {
      setError("Error al obtener los detalles del agente.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card
      variant="outlined"
      sx={{
        borderRadius: 2.5,
        height: "100%",
        transition: "transform 120ms ease, box-shadow 120ms ease, border-color 120ms ease",
        "&:hover": {
          transform: "translateY(-2px)",
          boxShadow: 3,
          borderColor: "divider",
        },
      }}
    >
      <CardContent sx={{ p: 2.25 }}>
        <Stack spacing={1.25}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="h6" sx={{ fontWeight: 900, lineHeight: 1.2 }} noWrap>
                {name}
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.75, mt: 0.25, maxWidth: "80%" }} noWrap>
                {description || "No description"}
              </Typography>
            </Box>
            <Chip label={label} color={color} variant="outlined" sx={{ fontWeight: 700 }} />
          </Stack>
          
          <Divider sx={{ my: 1 }} />

          {(metadata?.discovery?.protocol && metadata?.discovery?.endpoint) && (
            <Typography variant="body2" sx={{ fontSize: 12, opacity: 0.7 }}>
              {metadata.discovery.protocol}://{metadata.discovery.endpoint}
            </Typography>
          )}

          <Divider sx={{ my: 1 }} />

          <Stack direction="row" justifyContent="space-between">
            <Box>
              <Typography variant="caption" sx={{ opacity: 0.7 }}>Version</Typography>
              <Typography variant="body2" sx={{ fontWeight: 800 }}>
                {version || "—"}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" sx={{ opacity: 0.7 }}>Updated</Typography>
              <Typography variant="body2" sx={{ fontWeight: 800 }}>
                {formatDate(updatedTs)}
              </Typography>
            </Box>
          </Stack>

          <Typography variant="caption" sx={{ opacity: 0.55 }}>
            ID: {String(agentId).slice(-10)}
          </Typography>
        </Stack>
      </CardContent>

      <CardActions sx={{ px: 2.25, pb: 2, pt: 0, gap: 1 }}>
        {onTest && (
          <Button
            size="small"
            variant="outlined"
            startIcon={<WifiTetheringIcon />}
            onClick={() => onTest(agent)}
            sx={{ borderRadius: 2 }}
          >
            Test
          </Button>
        )}

      <Tooltip title="View Agent Definition">
  <IconButton size="small" onClick={handleViewAgentDefinition}>
    <CodeIcon />
  </IconButton>
</Tooltip>



        {/* Edit */}
        <Tooltip title="Edit Agent">
          <IconButton size="small" onClick={() => onEdit(agent)}>
            <EditIcon />
          </IconButton>
        </Tooltip>

        {/* Delete */}
        <Tooltip title="Delete Agent">
          <IconButton size="small" onClick={() => onDelete(agent)}>
            <DeleteOutlineIcon />
          </IconButton>
        </Tooltip>
      </CardActions>

      {/* Dialog to view Agent Definition */}
      {openDialog && (
        <AgentDefinitionDialog
          open={openDialog}
          definition={agentDefinition}
          loading={loading}
          error={error}
          onClose={() => setOpenDialog(false)}
        />
      )}
    </Card>
  );
};

export default AgentCard;
