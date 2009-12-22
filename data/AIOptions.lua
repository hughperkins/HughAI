--
-- Custom Options Definition Table format
--
-- A detailed example of how this format works can be found
-- in the spring source under:
-- AI/Skirmish/NullAI/data/AIOptions.lua
--
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

local options = {

	{
		key  = 'maphack',
		name = 'Activatd Maphack',
		desc = 'does the AI use maphack to find your units?',
		type = 'bool',
		def  = true,
	},

	{
		key  = 'guiactivated',
		name = 'Activate GUI',
		desc = 'Activates GUI. Mostly useful for developers. Make sure to play in windowed mode if this is activated!',
		type = 'bool',
		def  = true,
	},

	{
		key  = 'debug',
		name = 'Activated debug',
		desc = 'Shows lots of debug info, lines, ghost units and so on, so you can see what the AI is thinking. Turn off for normal usage.',
		type = 'bool',
		def  = false,
	},

}

return options
