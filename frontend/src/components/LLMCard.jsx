import React, { useState } from "react";
import {
  Card, CardContent, CardActions,
  Typography, Stack, Chip, Tooltip, Divider, Box, IconButton
} from "@mui/material";
import CodeIcon from "@mui/icons-material/Code";  // Para ver detalles del modelo
import { listAIModels } from "../api/AIModels";  // Asegúrate de importar el servicio adecuado
import AIModelDefinitionDialog from "./AIModelDefinitionDialog";  // Asegúrate de importar el componente de diálogo

const getStatusLabel = (status) => {
  switch (status) {
    case "ACTIVE":
      return { label: "Active", color: "success" };
    case "FAILED":
      return { label: "Failed", color: "error" };
    case "DISABLED":
      return { label: "Disabled", color: "default" };
    case "UNKNOWN":
    default:
      return { label: "Active", color: "success" };
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

const LLMCard = ({ llm }) => {
  const { modelId, modelName, modelDescription, updatedTs, status, provider } = llm;  // Asegúrate de destructurar provider
  const { label, color } = getStatusLabel(status);

  const [openDialog, setOpenDialog] = useState(false);
  const [llmDefinition, setLLMDefinition] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Función para abrir el diálogo y obtener la definición del modelo LLM
  const handleViewLLMDefinition = async () => {
    setLoading(true);
    setError(null);  // Limpiar cualquier error previo
    try {
      const llmDetails = await listAIModels(modelId);  // Usar el servicio para obtener detalles del modelo LLM
      setLLMDefinition(llmDetails);  // Establecer la definición del modelo LLM
      setOpenDialog(true);  // Abrir el diálogo
    } catch (err) {
      setError("Error al obtener los detalles del modelo LLM.");
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
                {modelName}
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.75, mt: 0.25, maxWidth: "80%" }} noWrap>
                {modelDescription || "No description"}
              </Typography>
            </Box>
            <Chip label={label} color={color} variant="outlined" sx={{ fontWeight: 700 }} />
          </Stack>

          <Divider sx={{ my: 1 }} />

          <Stack direction="row" justifyContent="space-between">
            <Box>
              <Typography variant="caption" sx={{ opacity: 0.7 }}>Updated</Typography>
              <Typography variant="body2" sx={{ fontWeight: 800 }}>
                {formatDate(updatedTs)}
              </Typography>
            </Box>
          </Stack>

          <Stack direction="row" justifyContent="space-between">
            <Box>
              <Typography variant="caption" sx={{ opacity: 0.7 }}>Provider</Typography>
              <Typography variant="body2" sx={{ fontWeight: 800 }}>
                {provider || "N/A"}  {/* Mostrar el proveedor */}
              </Typography>
            </Box>
          </Stack>

          <Typography variant="caption" sx={{ opacity: 0.55 }}>
            ID: {String(modelId).slice(-10)}
          </Typography>
        </Stack>
      </CardContent>

      <CardActions sx={{ px: 2.25, pb: 2, pt: 0, gap: 1 }}>
        {/* Ver definición */}
        <Tooltip title="View LLM Definition">
          <IconButton size="small" onClick={handleViewLLMDefinition}>
            <CodeIcon />
          </IconButton>
        </Tooltip>
      </CardActions>

      {/* Dialog to view LLM Definition */}
      {openDialog && (
        <AIModelDefinitionDialog
          open={openDialog}
          definition={llmDefinition}
          loading={loading}
          error={error}
          onClose={() => setOpenDialog(false)}
        />
      )}
    </Card>
  );
};

export default LLMCard;
