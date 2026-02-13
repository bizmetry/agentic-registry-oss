import React, { useState, useEffect, useMemo } from "react";
import {
  listAgents,
  deleteAgent,
  updateAgent,
  createAgent
} from "../../api/agents";

import { safe } from "../../api/client";

import {
  Grid,
  Button,
  Pagination,
  Stack,
  TextField,
  MenuItem,
  InputAdornment,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Typography,
  Alert,
  IconButton
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import CloseIcon from "@mui/icons-material/Close";

import AgentCard from "../../components/AgentCard";
import AgentForm from "./AgentForm";
import { ImportAgentDialog } from "../../components/ImportAgentDialog";

const PAGE_SIZE = 8;

// ===============================
// Sorting options
// ===============================
const SORT_OPTIONS = [
  { label: "Name (A → Z)", sortBy: "name", sortDir: "asc" },
  { label: "Name (Z → A)", sortBy: "name", sortDir: "desc" },
  { label: "Created (Oldest)", sortBy: "timestamp", sortDir: "asc" },
  { label: "Created (Newest)", sortBy: "timestamp", sortDir: "desc" }
];

// Debounce delay (ms)
const SEARCH_DELAY = 500;

export default function AgentView() {
  const [items, setItems] = useState([]);
  const [models, setModels] = useState([]); // ✅ (si lo usás en AgentForm)
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);

  const [open, setOpen] = useState(false);
  const [edit, setEdit] = useState(null);
  const [importDialogOpen, setImportDialogOpen] = useState(false);

  // ===============================
  // Delete confirmation dialog state
  // ===============================
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  // ===============================
  // Search + Sorting state
  // ===============================
  const [searchInput, setSearchInput] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [sortValue, setSortValue] = useState(SORT_OPTIONS[3]);

  // ===============================
  // Debounce search (type-ahead)
  // ===============================
  useEffect(() => {
    const t = setTimeout(() => {
      setSearchQuery(searchInput.trim());
      setPage(1);
    }, SEARCH_DELAY);

    return () => clearTimeout(t);
  }, [searchInput]);

  // ===============================
  // Load Agents from backend
  // ===============================
  const load = async () => {
    setLoading(true);

    const params = {
      search: searchQuery || undefined,
      sortBy: sortValue.sortBy,
      sortDir: sortValue.sortDir
    };

    const r = await safe(() => listAgents(params));

    if (r.ok) {
      const agents = Array.isArray(r.data) ? r.data : [];
      setItems(agents);
    } else {
      console.error("Error al cargar agentes:", r.error);
      setItems([]);
    }

    setLoading(false);
  };

  useEffect(() => {
    load();
  }, [searchQuery, sortValue]);

  // ===============================
  // Pagination slice
  // ===============================
  const slice = useMemo(() => {
    if (Array.isArray(items)) {
      return items.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
    }
    return [];
  }, [items, page]);

  // ===============================
  // Handle import
  // ===============================
  const onImported = (res) => {
    console.log("Import result:", res);
    load();
  };

  // ===============================
  // Delete flow
  // ===============================
  const openDeleteDialog = (agent) => {
    setDeleteTarget(agent);
    setDeleteError(null);
    setDeleteOpen(true);
  };

  const closeDeleteDialog = () => {
    if (deleting) return;
    setDeleteOpen(false);
    setDeleteTarget(null);
    setDeleteError(null);
  };

  const confirmDelete = async () => {
    if (!deleteTarget?.agentId) return;

    setDeleting(true);
    setDeleteError(null);

    const r = await safe(() => deleteAgent(deleteTarget.agentId));

    if (r.ok) {
      // optimista: remover de la lista actual
      setItems((prev) =>
        Array.isArray(prev) ? prev.filter((a) => a.agentId !== deleteTarget.agentId) : prev
      );
      setDeleting(false);
      setDeleteOpen(false);
      setDeleteTarget(null);

      // si preferís refetch completo:
      // await load();
    } else {
      const msg =
        r.error?.response?.data?.message ||
        r.error?.response?.data?.error ||
        r.error?.message ||
        "Failed to delete agent.";
      setDeleteError(msg);
      setDeleting(false);
    }
  };

  // ===============================
  // Create / Edit helpers
  // ===============================
  const openCreate = () => {
    setEdit(null);
    setOpen(true);
  };

  const openEdit = (agent) => {
    setEdit(agent);
    setOpen(true);
  };

  const closeForm = () => {
    setOpen(false);
    setEdit(null); // ✅ clave: limpiar modo edición al cerrar
  };

  return (
    <div style={{ padding: 20 }}>
      {/* ========================= */}
      {/* Top controls */}
      {/* ========================= */}
      <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
        <Button
          variant="contained"
          onClick={openCreate}
          sx={{ fontWeight: 900, padding: "8px 16px" }}
        >
          + Agent
        </Button>

        <Button
          variant="outlined"
          onClick={() => setImportDialogOpen(true)}
          sx={{ fontWeight: 900, padding: "8px 16px" }}
        >
          Import Agent
        </Button>
      </Stack>

      {/* ========================= */}
      {/* Search and sorting controls */}
      {/* ========================= */}
      <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
        <TextField
          fullWidth
          placeholder="Search agent..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon sx={{ opacity: 0.6 }} />
              </InputAdornment>
            ),
            endAdornment: loading ? (
              <InputAdornment position="end">
                <CircularProgress size={18} />
              </InputAdornment>
            ) : null
          }}
        />

        <TextField
          select
          label="Sort by"
          value={SORT_OPTIONS.indexOf(sortValue)}
          onChange={(e) => {
            setSortValue(SORT_OPTIONS[e.target.value]);
            setPage(1);
          }}
          sx={{ minWidth: 200 }}
        >
          {SORT_OPTIONS.map((opt, idx) => (
            <MenuItem key={idx} value={idx}>
              {opt.label}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {/* ========================= */}
      {/* Cards grid */}
      {/* ========================= */}
      <Grid container spacing={2} sx={{ mt: 2 }}>
        {slice.map((s) => (
          <Grid item xs={12} sm={6} md={3} key={s.agentId}>
            <AgentCard
              agent={s}
              onEdit={openEdit}                      // ✅ EDIT
              onDelete={openDeleteDialog}            // ✅ DELETE con confirm
              onTest={null}
            />
          </Grid>
        ))}

        {!loading && slice.length === 0 && (
          <Grid item xs={12} sx={{ opacity: 0.7, textAlign: "center", mt: 4 }}>
            No Agents found
          </Grid>
        )}
      </Grid>

      {/* ========================= */}
      {/* Pagination */}
      {/* ========================= */}
      {items.length > PAGE_SIZE && (
        <Stack alignItems="center" sx={{ mt: 3 }}>
          <Pagination
            count={Math.ceil(items.length / PAGE_SIZE)}
            page={page}
            onChange={(_, p) => setPage(p)}
            color="primary"
          />
        </Stack>
      )}

      {/* ========================= */}
      {/* Form dialog (Create/Edit) */}
      {/* ========================= */}
      <AgentForm
        open={open}
        initial={edit}              // ✅ si hay edit, precarga el form
        modelOptions={models}       // ✅ si lo usás
        onClose={closeForm}         // ✅ limpia edit al cerrar
        onSave={async (p) => {
          // ✅ MISMO flujo: create o update según `edit`
          if (edit?.agentId) {
            await updateAgent(edit.agentId, p);
          } else {
            await createAgent(p);
          }

          closeForm();              // ✅ cierra + limpia edit
          load();
        }}
      />

      {/* ========================= */}
      {/* Import Dialog */}
      {/* ========================= */}
      <ImportAgentDialog
        open={importDialogOpen}
        onClose={() => setImportDialogOpen(false)}
        onImported={onImported}
      />

      {/* ========================= */}
      {/* Delete Confirm Dialog */}
      {/* ========================= */}
      <Dialog open={deleteOpen} onClose={closeDeleteDialog} maxWidth="xs" fullWidth>
        <DialogTitle
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            fontWeight: 900
          }}
        >
          Delete Agent
          <IconButton onClick={closeDeleteDialog} disabled={deleting} size="small">
            <CloseIcon />
          </IconButton>
        </DialogTitle>

        <DialogContent sx={{ pt: 1.5 }}>
          <Stack spacing={2}>
            <Typography variant="body2" sx={{ opacity: 0.85, whiteSpace: "pre-wrap" }}>
              {deleteTarget
                ? `Are you sure you want to delete:\n\n${deleteTarget.name} (v${deleteTarget.version})\nID: ${String(
                    deleteTarget.agentId
                  ).slice(-10)}\n\nThis action cannot be undone.`
                : ""}
            </Typography>

            {deleteError ? <Alert severity="error">{deleteError}</Alert> : null}
          </Stack>
        </DialogContent>

        <DialogActions sx={{ px: 2.5, pb: 2 }}>
          <Button onClick={closeDeleteDialog} color="inherit" disabled={deleting}>
            Cancel
          </Button>

          <Button
            onClick={confirmDelete}
            variant="contained"
            color="error"
            disabled={deleting}
            sx={{ fontWeight: 900 }}
          >
            {deleting ? <CircularProgress size={18} color="inherit" /> : "Delete"}
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
}
