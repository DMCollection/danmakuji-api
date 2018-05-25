package cc.dmji.api.service.impl;

import cc.dmji.api.entity.Episode;
import cc.dmji.api.repository.EpisodeRepository;
import cc.dmji.api.service.EpisodeService;
import cc.dmji.api.utils.DmjiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class EpisodeServiceImpl implements EpisodeService {

    @Autowired
    private EpisodeRepository episodeRepository;

    @Override
    public List<Episode> listEpisodes() {
        return episodeRepository.findAll();
    }

    @Override
    public List<Episode> listEpisodesByBangumiId(Integer bangumiId) {
        List<Episode> result = null;
        result = episodeRepository.findEpisodesByBangumiIdEquals(bangumiId);
        return result;
    }

    @Override
    public Episode getEpisodeByBangumiIdAndEpIndex(Integer bangumiId, Integer epIndex) {
        return episodeRepository.findEpisodeByBangumiIdEqualsAndEpIndexEquals(bangumiId,epIndex);
    }

    @Override
    public Episode getEpisodeByEpId(Integer epId) {
        return episodeRepository.findById(epId).orElse(null);
    }

    @Override
    public Episode insertEpisode(Episode episode) {
        setCreateAndModifyTime(episode);
        episode.setDanmakuId(DmjiUtils.getUUID32());
        return episodeRepository.save(episode);
    }

    @Override
    public List<Episode> insertEpisodes(List<Episode> episodes) {
        episodes.forEach(e->{
            setCreateAndModifyTime(e);
            e.setDanmakuId(DmjiUtils.getUUID32());
        });
        return episodeRepository.saveAll(episodes);
    }

    @Override
    public Episode updateEpisode(Episode episode) {
        setModifyTime(episode);
        return episodeRepository.save(episode);
    }

    @Override
    public void deleteEpisode(Integer id) {
        episodeRepository.deleteById(id);
    }

    @Override
    public Long countEpisode() {
        return episodeRepository.count();
    }

    private void setModifyTime(Episode episode){
        Timestamp date = new Timestamp(System.currentTimeMillis());
        episode.setModifyTime(date);
    }

    private void setCreateAndModifyTime(Episode episode){
        Timestamp date = new Timestamp(System.currentTimeMillis());
        episode.setModifyTime(date);
        episode.setCreateTime(date);
    }
}
